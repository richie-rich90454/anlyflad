package com.anlyflad.desktop;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.File;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileNameExtensionFilter;
import com.anlyflad.core.stage.PipelineConfig;
import com.anlyflad.core.stage.StageRegistry;
public final class VectoriumFrame extends JFrame {
    private static final long serialVersionUID=1L;
    private final VectorCanvas canvas;
    private final DesktopToolbar toolbar;
    private final StageInspectorPanel stageInspector;
    private final StatusBar statusBar;
    private final JSplitPane splitPane;
    private final PipelineController controller;
    public VectoriumFrame() {
        super("Anlyflad");
        canvas=new VectorCanvas();
        toolbar=new DesktopToolbar();
        stageInspector=new StageInspectorPanel(new StageRegistry(), PipelineConfig.defaults());
        statusBar=new StatusBar();
        controller=new PipelineController(canvas, statusBar);
        splitPane=new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, canvas, stageInspector);
        splitPane.setResizeWeight(0.72);
        splitPane.setDividerLocation(760);
        splitPane.setContinuousLayout(true);
        splitPane.setOneTouchExpandable(true);
        splitPane.setBorder(null);
        JPanel content=new JPanel(new BorderLayout());
        content.setBackground(DesktopTheme.WINDOW);
        content.add(toolbar, BorderLayout.NORTH);
        content.add(splitPane, BorderLayout.CENTER);
        content.add(statusBar, BorderLayout.SOUTH);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(1280, 820);
        setMinimumSize(new Dimension(960, 640));
        setLocationRelativeTo(null);
        setContentPane(content);
        getAccessibleContext().setAccessibleName("Anlyflad desktop application");
        controller.setDocumentListener(new Runnable() {
            public void run() {
                boolean busy=controller.isBusy();
                toolbar.setRunning(busy);
                toolbar.setExportEnabled(!busy&&controller.getResult()!=null);
            }
        });
        stageInspector.setChangeListener(new Runnable() {
            public void run() {
                updatePipeline();
            }
        });
        toolbar.addOpenListener(new OpenListener());
        toolbar.addExportListener(new ExportListener());
        toolbar.addPresetListener(new PresetListener());
        toolbar.addModeListener(new ModeListener());
        toolbar.addVectorModeListener(new VectorModeListener());
        toolbar.addCleanListener(new CleanListener());
        addWindowListener(new WindowCloseListener(this));
    }
    public static void main(String[] args) {
        SwingUtilities.invokeLater(new LaunchFrame());
    }
    public VectorCanvas getCanvas() {
        return canvas;
    }
    public DesktopToolbar getToolbar() {
        return toolbar;
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
    public PipelineController getController() {
        return controller;
    }
    private void updatePipeline() {
        PipelineConfig base=PipelineConfig.defaults().withPreset(toolbar.getPreset()).withRasterMode(toolbar.getRasterMode()).withVectorMode(toolbar.getVectorMode()).withClean(toolbar.isClean());
        PipelineConfig configured=stageInspector.applyTo(base).withStageValue("vectorize", "mode", toolbar.getVectorMode().getOptionName());
        controller.setConfig(configured);
        controller.runPipeline();
    }
    private File chooseInput() {
        JFileChooser chooser=new JFileChooser();
        chooser.setDialogTitle("Open image or SVG");
        chooser.setFileFilter(new FileNameExtensionFilter("Images and SVG", "png", "jpg", "jpeg", "svg"));
        if (chooser.showOpenDialog(this)==JFileChooser.APPROVE_OPTION) {
            return chooser.getSelectedFile();
        }
        return null;
    }
    private File chooseOutput() {
        JFileChooser chooser=new JFileChooser();
        chooser.setDialogTitle("Export SVG");
        chooser.setFileFilter(new FileNameExtensionFilter("SVG", "svg"));
        if (chooser.showSaveDialog(this)==JFileChooser.APPROVE_OPTION) {
            return chooser.getSelectedFile();
        }
        return null;
    }
    private final class OpenListener implements ActionListener {
        public void actionPerformed(ActionEvent event) {
            File file=chooseInput();
            if (file!=null) {
                controller.load(file);
            }
        }
    }
    private final class ExportListener implements ActionListener {
        public void actionPerformed(ActionEvent event) {
            File file=chooseOutput();
            if (file!=null) {
                if (!file.getName().toLowerCase(java.util.Locale.ROOT).endsWith(".svg")) {
                    file=new File(file.getParentFile(), file.getName()+".svg");
                }
                controller.export(file);
            }
        }
    }
    private final class PresetListener implements ActionListener {
        public void actionPerformed(ActionEvent event) {
            updatePipeline();
        }
    }
    private final class ModeListener implements ActionListener {
        public void actionPerformed(ActionEvent event) {
            updatePipeline();
        }
    }
    private final class CleanListener implements ActionListener {
        public void actionPerformed(ActionEvent event) {
            updatePipeline();
        }
    }
    private final class VectorModeListener implements ActionListener {
        public void actionPerformed(ActionEvent event) {
            updatePipeline();
        }
    }
    private static final class WindowCloseListener implements WindowListener {
        private final VectoriumFrame frame;
        private WindowCloseListener(VectoriumFrame frame) {
            this.frame=frame;
        }
        public void windowClosing(WindowEvent event) {
            frame.getController().close();
        }
        public void windowOpened(WindowEvent event) {
        }
        public void windowClosed(WindowEvent event) {
        }
        public void windowIconified(WindowEvent event) {
        }
        public void windowDeiconified(WindowEvent event) {
        }
        public void windowActivated(WindowEvent event) {
        }
        public void windowDeactivated(WindowEvent event) {
        }
    }
    private static final class LaunchFrame implements Runnable {
        public void run() {
            new VectoriumFrame().setVisible(true);
        }
    }
}
