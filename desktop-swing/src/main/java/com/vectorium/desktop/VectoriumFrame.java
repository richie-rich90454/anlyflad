package com.vectorium.desktop;
import java.awt.BorderLayout;
import java.awt.Dimension;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.SwingUtilities;
import com.vectorium.core.stage.PipelineConfig;
import com.vectorium.core.stage.StageRegistry;
public final class VectoriumFrame extends JFrame {
    private static final long serialVersionUID=1L;
    private final VectorCanvas canvas;
    private final StageInspectorPanel stageInspector;
    private final StatusBar statusBar;
    private final JSplitPane splitPane;
    public VectoriumFrame() {
        super("Anlyflad");
        canvas=new VectorCanvas();
        stageInspector=new StageInspectorPanel(new StageRegistry(), PipelineConfig.defaults());
        statusBar=new StatusBar();
        splitPane=new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, canvas, stageInspector);
        splitPane.setResizeWeight(0.72);
        splitPane.setDividerLocation(760);
        splitPane.setContinuousLayout(true);
        splitPane.setOneTouchExpandable(true);
        splitPane.setBorder(null);
        JPanel content=new JPanel(new BorderLayout());
        content.setBackground(DesktopTheme.WINDOW);
        content.add(splitPane, BorderLayout.CENTER);
        content.add(statusBar, BorderLayout.SOUTH);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(1280, 820);
        setMinimumSize(new Dimension(960, 640));
        setLocationRelativeTo(null);
        setContentPane(content);
        getAccessibleContext().setAccessibleName("Anlyflad desktop application");
    }
    public static void main(String[] args) {
        SwingUtilities.invokeLater(new LaunchFrame());
    }
    public VectorCanvas getCanvas() {
        return canvas;
    }
    public StageInspectorPanel getStageInspector() {
        return stageInspector;
    }
    public StatusBar getStatusBar() {
        return statusBar;
    }
    public JSplitPane getSplitPane() {
        return splitPane;
    }
    private static final class LaunchFrame implements Runnable {
        public void run() {
            new VectoriumFrame().setVisible(true);
        }
    }
}
