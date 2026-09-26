package com.anlyflad.desktop;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionListener;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import com.anlyflad.core.stage.RasterMode;
import com.anlyflad.core.stage.VectorMode;
public final class DesktopToolbar extends JPanel {
    private static final long serialVersionUID=1L;
    private final JButton openButton;
    private final JButton exportButton;
    private final JButton runButton;
    private final JButton fitButton;
    private final JButton actualSizeButton;
    private final JButton zoomOutButton;
    private final JButton zoomInButton;
    private final JComboBox<String> preset;
    private final JComboBox<String> mode;
    private final JComboBox<String> vectorMode;
    private final JSlider quality;
    private final JCheckBox clean;
    private final JLabel modeHint;
    private final JLabel qualityValue;
    private final JLabel zoomLabel;
    private boolean exportAllowed;
    private boolean runAllowed;
    private boolean running;
    public DesktopToolbar() {
        openButton=new JButton("Open image");
        exportButton=new JButton("Export SVG");
        runButton=new JButton("Run");
        fitButton=new JButton("Fit");
        actualSizeButton=new JButton("1:1");
        zoomOutButton=new JButton("-");
        zoomInButton=new JButton("+");
        exportButton.setEnabled(false);
        runButton.setEnabled(false);
        preset=new JComboBox<String>(new String[]{"default", "clean", "fast", "accurate"});
        mode=new JComboBox<String>(new String[]{RasterMode.COLOR.getOptionName(), RasterMode.BINARY.getOptionName()});
        vectorMode=new JComboBox<String>(new String[]{VectorMode.CURVE.getOptionName(), VectorMode.CONTOUR.getOptionName(), VectorMode.EXACT.getOptionName()});
        quality=new JSlider(0, 100, 50);
        clean=new JCheckBox("Enabled", true);
        modeHint=new JLabel(" ");
        modeHint.setFont(DesktopTheme.CAPTION_FONT);
        modeHint.setForeground(DesktopTheme.MUTED_TEXT);
        qualityValue=new JLabel("50");
        qualityValue.setFont(DesktopTheme.LABEL_FONT);
        qualityValue.setForeground(DesktopTheme.TEXT);
        zoomLabel=new JLabel("100%");
        zoomLabel.setFont(DesktopTheme.LABEL_FONT);
        zoomLabel.setForeground(DesktopTheme.TEXT);
        setLayout(new BorderLayout(18, 10));
        setBackground(DesktopTheme.SURFACE);
        setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, DesktopTheme.BORDER), BorderFactory.createEmptyBorder(12, 14, 10, 14)));
        add(createActionGroup(), BorderLayout.WEST);
        add(createSettingsGroup(), BorderLayout.CENTER);
        add(createViewGroup(), BorderLayout.EAST);
        JPanel south=new JPanel(new BorderLayout(0, 2));
        south.setOpaque(false);
        south.add(createQualityGroup(), BorderLayout.NORTH);
        south.add(modeHint, BorderLayout.SOUTH);
        add(south, BorderLayout.SOUTH);
        stylePrimary(openButton);
        styleSecondary(exportButton);
        styleAccent(runButton);
        styleSecondary(fitButton);
        styleSecondary(actualSizeButton);
        styleSecondary(zoomOutButton);
        styleSecondary(zoomInButton);
        openButton.setPreferredSize(new Dimension(126, 38));
        exportButton.setPreferredSize(new Dimension(126, 38));
        runButton.setPreferredSize(new Dimension(84, 34));
        fitButton.setPreferredSize(new Dimension(56, 30));
        actualSizeButton.setPreferredSize(new Dimension(56, 30));
        zoomOutButton.setPreferredSize(new Dimension(38, 30));
        zoomInButton.setPreferredSize(new Dimension(38, 30));
        preset.setPreferredSize(new Dimension(132, 30));
        mode.setPreferredSize(new Dimension(132, 30));
        vectorMode.setPreferredSize(new Dimension(132, 30));
        quality.setPreferredSize(new Dimension(360, 44));
        quality.setBackground(DesktopTheme.SURFACE);
        quality.setMajorTickSpacing(50);
        quality.setMinorTickSpacing(10);
        quality.setPaintTicks(true);
        quality.setPaintLabels(true);
        quality.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent event) {
                qualityValue.setText(Integer.toString(quality.getValue()));
            }
        });
        openButton.getAccessibleContext().setAccessibleName("Open image");
        exportButton.getAccessibleContext().setAccessibleName("Export SVG");
        runButton.getAccessibleContext().setAccessibleName("Run pipeline");
        fitButton.getAccessibleContext().setAccessibleName("Fit image to window");
        actualSizeButton.getAccessibleContext().setAccessibleName("Show at actual size");
        zoomInButton.getAccessibleContext().setAccessibleName("Zoom in");
        zoomOutButton.getAccessibleContext().setAccessibleName("Zoom out");
        preset.getAccessibleContext().setAccessibleName("Pipeline preset");
        mode.getAccessibleContext().setAccessibleName("Raster mode");
        vectorMode.getAccessibleContext().setAccessibleName("Vector mode");
        quality.getAccessibleContext().setAccessibleName("Quality scale");
        quality.setToolTipText("0 = smallest SVG, 100 = most detailed SVG");
        clean.getAccessibleContext().setAccessibleName("Enable clean output");
        updateModeHint();
        vectorMode.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent event) {
                updateModeHint();
            }
        });
    }
    private JPanel createActionGroup() {
        JPanel group=new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        group.setOpaque(false);
        group.add(openButton);
        group.add(exportButton);
        group.add(runButton);
        return group;
    }
    private JPanel createSettingsGroup() {
        JPanel group=new JPanel(new FlowLayout(FlowLayout.LEFT, 14, 0));
        group.setOpaque(false);
        group.add(field("Preset", preset));
        group.add(field("Raster mode", mode));
        group.add(field("Vector mode", vectorMode));
        JPanel cleanField=new JPanel();
        cleanField.setOpaque(false);
        cleanField.setLayout(new BoxLayout(cleanField, BoxLayout.Y_AXIS));
        JLabel caption=new JLabel("Cleanup");
        caption.setFont(DesktopTheme.CAPTION_FONT);
        caption.setForeground(DesktopTheme.MUTED_TEXT);
        cleanField.add(caption);
        cleanField.add(clean);
        clean.setFont(DesktopTheme.BODY_FONT);
        clean.setBackground(DesktopTheme.SURFACE);
        clean.setForeground(DesktopTheme.TEXT);
        group.add(cleanField);
        return group;
    }
    private JPanel createQualityGroup() {
        JPanel group=new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        group.setOpaque(false);
        JLabel caption=new JLabel("Quality scale");
        caption.setFont(DesktopTheme.CAPTION_FONT);
        caption.setForeground(DesktopTheme.MUTED_TEXT);
        JLabel range=new JLabel("0 = smallest, 100 = most detailed");
        range.setFont(DesktopTheme.CAPTION_FONT);
        range.setForeground(DesktopTheme.MUTED_TEXT);
        group.add(caption);
        group.add(quality);
        group.add(qualityValue);
        group.add(range);
        return group;
    }
    private JPanel createViewGroup() {
        JPanel group=new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        group.setOpaque(false);
        group.add(field("View", fitButton));
        group.add(actualSizeButton);
        group.add(zoomOutButton);
        group.add(zoomLabel);
        group.add(zoomInButton);
        return group;
    }
    private static JPanel field(String captionText, java.awt.Component component) {
        JPanel field=new JPanel();
        field.setOpaque(false);
        field.setLayout(new BoxLayout(field, BoxLayout.Y_AXIS));
        JLabel caption=new JLabel(captionText);
        caption.setFont(DesktopTheme.CAPTION_FONT);
        caption.setForeground(DesktopTheme.MUTED_TEXT);
        field.add(caption);
        field.add(component);
        return field;
    }
    private void updateModeHint() {
        VectorMode value=getVectorMode();
        if (value==VectorMode.CURVE) {
            modeHint.setText("Curves: smooth, resolution-independent outlines.");
        } else if (value==VectorMode.CONTOUR) {
            modeHint.setText("Contour: simplified color regions without curve fitting.");
        } else {
            modeHint.setText("Exact runs: pixel-faithful rectangles for lossless color.");
        }
    }
    private static void stylePrimary(JButton button) {
        styleBase(button);
        button.setBackground(DesktopTheme.ACCENT);
        button.setForeground(DesktopTheme.PAGE);
    }
    private static void styleAccent(JButton button) {
        styleBase(button);
        button.setBackground(DesktopTheme.TEXT);
        button.setForeground(DesktopTheme.PAGE);
    }
    private static void styleSecondary(JButton button) {
        styleBase(button);
        button.setBackground(DesktopTheme.SURFACE);
        button.setForeground(DesktopTheme.TEXT);
        button.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(DesktopTheme.BORDER), BorderFactory.createEmptyBorder(6, 12, 6, 12)));
    }
    private static void styleBase(JButton button) {
        button.setFont(DesktopTheme.LABEL_FONT);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createEmptyBorder(8, 14, 8, 14));
        button.setOpaque(true);
        button.setContentAreaFilled(true);
    }
    public void addOpenListener(ActionListener listener) {
        openButton.addActionListener(listener);
    }
    public void addExportListener(ActionListener listener) {
        exportButton.addActionListener(listener);
    }
    public void addRunListener(ActionListener listener) {
        runButton.addActionListener(listener);
    }
    public void addFitListener(ActionListener listener) {
        fitButton.addActionListener(listener);
    }
    public void addActualSizeListener(ActionListener listener) {
        actualSizeButton.addActionListener(listener);
    }
    public void addZoomInListener(ActionListener listener) {
        zoomInButton.addActionListener(listener);
    }
    public void addZoomOutListener(ActionListener listener) {
        zoomOutButton.addActionListener(listener);
    }
    public void addPresetListener(ActionListener listener) {
        preset.addActionListener(listener);
    }
    public void addModeListener(ActionListener listener) {
        mode.addActionListener(listener);
    }
    public void addCleanListener(ActionListener listener) {
        clean.addActionListener(listener);
    }
    public void addVectorModeListener(ActionListener listener) {
        vectorMode.addActionListener(listener);
    }
    public void addQualityListener(final ActionListener listener) {
        quality.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent event) {
                listener.actionPerformed(new java.awt.event.ActionEvent(quality, java.awt.event.ActionEvent.ACTION_PERFORMED, Integer.toString(quality.getValue())));
            }
        });
    }
    public int getQuality() {
        return quality.getValue();
    }
    public void setQuality(int value) {
        quality.setValue(value);
    }
    public void setZoomLabel(String text) {
        zoomLabel.setText(text);
    }
    public void setExportEnabled(boolean enabled) {
        exportAllowed=enabled;
        exportButton.setEnabled(enabled&&!running);
    }
    public void setRunEnabled(boolean enabled) {
        runAllowed=enabled;
        runButton.setEnabled(enabled&&!running);
    }
    public void setRunning(boolean running) {
        this.running=running;
        openButton.setEnabled(!running);
        exportButton.setEnabled(!running&&exportAllowed);
        runButton.setEnabled(!running&&runAllowed);
        preset.setEnabled(!running);
        mode.setEnabled(!running);
        vectorMode.setEnabled(!running);
        quality.setEnabled(!running);
        clean.setEnabled(!running);
    }
    public String getPreset() {
        return (String)preset.getSelectedItem();
    }
    public void setPreset(String name) {
        preset.setSelectedItem(name);
    }
    public RasterMode getRasterMode() {
        return RasterMode.parse((String)mode.getSelectedItem());
    }
    public void setRasterMode(RasterMode rasterMode) {
        if (rasterMode==null) {
            throw new IllegalArgumentException("rasterMode must not be null");
        }
        mode.setSelectedItem(rasterMode.getOptionName());
    }
    public VectorMode getVectorMode() {
        return VectorMode.parse((String)vectorMode.getSelectedItem());
    }
    public void setVectorMode(VectorMode vectorMode) {
        if (vectorMode==null) {
            throw new IllegalArgumentException("vectorMode must not be null");
        }
        this.vectorMode.setSelectedItem(vectorMode.getOptionName());
        updateModeHint();
    }
    public boolean isClean() {
        return clean.isSelected();
    }
    public void setClean(boolean value) {
        clean.setSelected(value);
    }
}
