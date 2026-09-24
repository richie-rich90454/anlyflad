package com.vectorium.desktop;
import java.awt.Dimension;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
public final class VectoriumFrame extends JFrame {
    private static final long serialVersionUID=1L;
    private final VectorCanvas canvas;
    public VectoriumFrame() {
        super("Anlyflad");
        canvas=new VectorCanvas();
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(1280, 820);
        setMinimumSize(new Dimension(960, 640));
        setLocationRelativeTo(null);
        getContentPane().setBackground(DesktopTheme.WINDOW);
        setContentPane(canvas);
        getAccessibleContext().setAccessibleName("Anlyflad desktop application");
    }
    public static void main(String[] args) {
        SwingUtilities.invokeLater(new LaunchFrame());
    }
    public VectorCanvas getCanvas() {
        return canvas;
    }
    private static final class LaunchFrame implements Runnable {
        public void run() {
            new VectoriumFrame().setVisible(true);
        }
    }
}
