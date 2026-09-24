package com.vectorium.desktop;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionListener;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JPanel;
public final class DesktopToolbar extends JPanel {
    private static final long serialVersionUID=1L;
    private final JButton openButton;
    private final JButton exportButton;
    private final JComboBox<String> preset;
    private final JCheckBox clean;
    public DesktopToolbar() {
        openButton=new JButton("Open image");
        exportButton=new JButton("Export SVG");
        exportButton.setEnabled(false);
        preset=new JComboBox<String>(new String[]{"default", "clean", "fast", "accurate"});
        clean=new JCheckBox("Clean output", true);
        setLayout(new FlowLayout(FlowLayout.LEFT, 8, 8));
        setBackground(DesktopTheme.SURFACE);
        setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, DesktopTheme.BORDER));
        add(openButton);
        add(exportButton);
        add(new javax.swing.JLabel("Preset"));
        add(preset);
        add(clean);
        openButton.setPreferredSize(new Dimension(112, 36));
        exportButton.setPreferredSize(new Dimension(112, 36));
        openButton.getAccessibleContext().setAccessibleName("Open image");
        exportButton.getAccessibleContext().setAccessibleName("Export SVG");
        preset.getAccessibleContext().setAccessibleName("Pipeline preset");
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
    public void addCleanListener(ActionListener listener) {
        clean.addActionListener(listener);
    }
    public void setExportEnabled(boolean enabled) {
        exportButton.setEnabled(enabled);
    }
    public void setRunning(boolean running) {
        openButton.setEnabled(!running);
        exportButton.setEnabled(!running&&exportButton.isEnabled());
    }
    public String getPreset() {
        return (String)preset.getSelectedItem();
    }
    public void setPreset(String name) {
        preset.setSelectedItem(name);
    }
    public boolean isClean() {
        return clean.isSelected();
    }
    public void setClean(boolean value) {
        clean.setSelected(value);
    }
}
