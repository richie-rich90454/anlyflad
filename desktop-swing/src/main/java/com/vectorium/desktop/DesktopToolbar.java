package com.vectorium.desktop;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionListener;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import com.vectorium.core.stage.RasterMode;
import com.vectorium.core.stage.VectorMode;
public final class DesktopToolbar extends JPanel {
    private static final long serialVersionUID=1L;
    private final JButton openButton;
    private final JButton exportButton;
    private final JComboBox<String> preset;
    private final JComboBox<String> mode;
    private final JComboBox<String> vectorMode;
    private final JCheckBox clean;
    private boolean exportAllowed;
    private boolean running;
    public DesktopToolbar() {
        openButton=new JButton("Open image");
        exportButton=new JButton("Export SVG");
        exportButton.setEnabled(false);
        preset=new JComboBox<String>(new String[]{"default", "clean", "fast", "accurate"});
        mode=new JComboBox<String>(new String[]{RasterMode.COLOR.getOptionName(), RasterMode.BINARY.getOptionName()});
        vectorMode=new JComboBox<String>(new String[]{VectorMode.CURVE.getOptionName(), VectorMode.CONTOUR.getOptionName(), VectorMode.EXACT.getOptionName()});
        clean=new JCheckBox("Clean output", true);
        setLayout(new FlowLayout(FlowLayout.LEFT, 8, 8));
        setBackground(DesktopTheme.SURFACE);
        setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, DesktopTheme.BORDER));
        add(openButton);
        add(exportButton);
        add(new javax.swing.JLabel("Preset"));
        add(preset);
        add(new javax.swing.JLabel("Raster mode"));
        add(mode);
        add(new javax.swing.JLabel("Vector mode"));
        add(vectorMode);
        add(clean);
        openButton.setPreferredSize(new Dimension(112, 36));
        exportButton.setPreferredSize(new Dimension(112, 36));
        openButton.getAccessibleContext().setAccessibleName("Open image");
        exportButton.getAccessibleContext().setAccessibleName("Export SVG");
        preset.getAccessibleContext().setAccessibleName("Pipeline preset");
        mode.getAccessibleContext().setAccessibleName("Raster mode");
        vectorMode.getAccessibleContext().setAccessibleName("Vector mode");
        clean.getAccessibleContext().setAccessibleName("Enable clean output");
    }
    public void addOpenListener(ActionListener listener) {
        openButton.addActionListener(listener);
    }
    public void addExportListener(ActionListener listener) {
        exportButton.addActionListener(listener);
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
    public void setExportEnabled(boolean enabled) {
        exportAllowed=enabled;
        exportButton.setEnabled(enabled&&!running);
    }
    public void setRunning(boolean running) {
        this.running=running;
        openButton.setEnabled(!running);
        exportButton.setEnabled(!running&&exportAllowed);
        preset.setEnabled(!running);
        mode.setEnabled(!running);
        vectorMode.setEnabled(!running);
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
    }
    public boolean isClean() {
        return clean.isSelected();
    }
    public void setClean(boolean value) {
        clean.setSelected(value);
    }
}
