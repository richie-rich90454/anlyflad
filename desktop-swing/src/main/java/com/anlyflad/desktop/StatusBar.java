package com.anlyflad.desktop;
import java.awt.BorderLayout;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
public final class StatusBar extends JPanel {
    private static final long serialVersionUID=1L;
    private final JLabel status;
    private final JLabel detail;
    private final JProgressBar progress;
    public StatusBar() {
        status=new JLabel("Ready");
        detail=new JLabel("No image loaded");
        detail.setFont(DesktopTheme.CAPTION_FONT);
        detail.setForeground(DesktopTheme.MUTED_TEXT);
        status.setFont(DesktopTheme.BODY_FONT);
        status.setForeground(DesktopTheme.TEXT);
        status.getAccessibleContext().setAccessibleName("Application status");
        progress=new JProgressBar(0, 100);
        progress.setStringPainted(false);
        progress.setVisible(false);
        progress.setPreferredSize(new java.awt.Dimension(180, 8));
        progress.getAccessibleContext().setAccessibleName("Pipeline progress");
        setLayout(new BorderLayout(12, 0));
        setBackground(DesktopTheme.SURFACE);
        setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, DesktopTheme.BORDER), BorderFactory.createEmptyBorder(8, 12, 8, 12)));
        add(status, BorderLayout.WEST);
        add(detail, BorderLayout.CENTER);
        add(progress, BorderLayout.EAST);
    }
    public void setStatus(String text) {
        if (text==null||text.trim().isEmpty()) {
            throw new IllegalArgumentException("status text must not be blank");
        }
        status.setText(text);
    }
    public String getStatusText() {
        return status.getText();
    }
    public void setDetail(String text) {
        detail.setText(text==null||text.trim().isEmpty()?" ":text);
    }
    public String getDetailText() {
        return detail.getText();
    }
    public void setProgress(int value, int maximum) {
        if (value<0||maximum<=0||value>maximum) {
            throw new IllegalArgumentException("progress must satisfy 0 <= value <= maximum");
        }
        progress.setIndeterminate(false);
        progress.setMaximum(maximum);
        progress.setValue(value);
        progress.setVisible(true);
    }
    public int getProgressValue() {
        return progress.getValue();
    }
    public int getProgressMaximum() {
        return progress.getMaximum();
    }
    public void setBusy(boolean busy) {
        progress.setIndeterminate(busy);
        progress.setVisible(busy);
    }
    public boolean isBusy() {
        return progress.isVisible();
    }
    public boolean isProgressIndeterminate() {
        return progress.isIndeterminate();
    }
}
