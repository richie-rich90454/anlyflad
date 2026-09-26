package com.anlyflad.desktop;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.filechooser.FileNameExtensionFilter;
import com.anlyflad.core.model.VectorDocument;
import com.anlyflad.core.stage.PipelineConfig;
import com.anlyflad.core.stage.QualityScale;
import com.anlyflad.core.stage.StageRegistry;
public final class VectoriumFrame extends JFrame {
    private static final long serialVersionUID=1L;
    static {
        java.util.Locale.setDefault(java.util.Locale.Category.DISPLAY, java.util.Locale.ENGLISH);
        java.util.Locale.setDefault(java.util.Locale.Category.FORMAT, java.util.Locale.ENGLISH);
        java.util.Locale.setDefault(java.util.Locale.ENGLISH);
        DesktopTheme.installUiFonts();
    }
    private static final double ZOOM_STEP=1.25;
    private final VectorCanvas canvas;
    private final DesktopToolbar toolbar;
    private final StageInspectorPanel stageInspector;
    private final StatusBar statusBar;
    private final JSplitPane splitPane;
    private final PipelineController controller;
    private final Timer pipelineDebounce;
    private DocumentationWindow documentationWindow;
    public VectoriumFrame() {
        super("Anlyflad");
        installIcon();
        canvas=new VectorCanvas();
        toolbar=new DesktopToolbar();
        stageInspector=new StageInspectorPanel(new StageRegistry(), PipelineConfig.defaults());
        statusBar=new StatusBar();
        controller=new PipelineController(canvas, statusBar);
        pipelineDebounce=new Timer(220, new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                updatePipeline();
            }
        });
        pipelineDebounce.setRepeats(false);
        splitPane=new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, canvas, stageInspector);
        splitPane.setResizeWeight(0.74);
        splitPane.setDividerLocation(840);
        splitPane.setContinuousLayout(true);
        splitPane.setOneTouchExpandable(true);
        splitPane.setBorder(null);
        JPanel content=new JPanel(new BorderLayout());
        content.setBackground(DesktopTheme.WINDOW);
        content.add(toolbar, BorderLayout.NORTH);
        content.add(splitPane, BorderLayout.CENTER);
        content.add(statusBar, BorderLayout.SOUTH);
        setJMenuBar(createMenuBar());
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
                toolbar.setRunEnabled(controller.getSource()!=null);
                VectorDocument source=controller.getSource();
                String name=source==null?null:source.getOrigin().getSourceName();
                setTitle(name==null?"Anlyflad":"Anlyflad - "+name);
                if (source!=null) {
                    statusBar.setDetail(source.getWidth()+" x "+source.getHeight());
                } else {
                    statusBar.setDetail("No image loaded");
                }
            }
        });
        stageInspector.setChangeListener(new Runnable() {
            public void run() {
                schedulePipelineUpdate();
            }
        });
        canvas.addPropertyChangeListener("zoom", new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent event) {
                updateZoomLabel();
            }
        });
        toolbar.addOpenListener(new OpenListener());
        toolbar.addExportListener(new ExportListener());
        toolbar.addRunListener(new RunListener());
        toolbar.addPresetListener(new PresetListener());
        toolbar.addModeListener(new ModeListener());
        toolbar.addVectorModeListener(new VectorModeListener());
        toolbar.addQualityListener(new QualityListener());
        toolbar.addCleanListener(new CleanListener());
        toolbar.addFitListener(new FitListener());
        toolbar.addActualSizeListener(new ActualSizeListener());
        toolbar.addZoomInListener(new ZoomInListener());
        toolbar.addZoomOutListener(new ZoomOutListener());
        addWindowListener(new WindowCloseListener(this));
        toolbar.setRunEnabled(false);
        updateZoomLabel();
    }
    public static void main(String[] args) {
        SwingUtilities.invokeLater(new LaunchFrame());
    }
    private void installIcon() {
        List<Image> icons=new ArrayList<Image>();
        String[] names={"anlyflad-icon-16.png", "anlyflad-icon-32.png", "anlyflad-icon-48.png", "anlyflad-icon-64.png", "anlyflad-icon-128.png", "anlyflad-icon-256.png"};
        for (int index=0;index<names.length;index++) {
            InputStream stream=VectoriumFrame.class.getResourceAsStream("/"+names[index]);
            if (stream==null) {
                continue;
            }
            try {
                Image image=ImageIO.read(stream);
                if (image!=null) {
                    icons.add(image);
                }
            } catch (IOException exception) {
                // ponytail: missing or unreadable icon sizes simply fall through to the remaining sizes
            } finally {
                try {
                    stream.close();
                } catch (IOException exception) {
                }
            }
        }
        if (!icons.isEmpty()) {
            setIconImages(icons);
        }
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
    private JMenuBar createMenuBar() {
        JMenuBar menuBar=new JMenuBar();
        JMenu file=new JMenu("File");
        file.setMnemonic(KeyEvent.VK_F);
        JMenuItem open=new JMenuItem("Open image...");
        open.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, InputEvent.CTRL_DOWN_MASK));
        open.addActionListener(new OpenListener());
        JMenuItem export=new JMenuItem("Export SVG...");
        export.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK));
        export.addActionListener(new ExportListener());
        JMenuItem exit=new JMenuItem("Exit");
        exit.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Q, InputEvent.CTRL_DOWN_MASK));
        exit.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                dispose();
            }
        });
        file.add(open);
        file.add(export);
        file.addSeparator();
        file.add(exit);
        JMenu view=new JMenu("View");
        view.setMnemonic(KeyEvent.VK_V);
        JMenuItem fit=new JMenuItem("Fit to window");
        fit.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_0, InputEvent.CTRL_DOWN_MASK));
        fit.addActionListener(new FitListener());
        JMenuItem actual=new JMenuItem("Actual size");
        actual.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_1, InputEvent.CTRL_DOWN_MASK));
        actual.addActionListener(new ActualSizeListener());
        JMenuItem zoomIn=new JMenuItem("Zoom in");
        zoomIn.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_EQUALS, InputEvent.CTRL_DOWN_MASK));
        zoomIn.addActionListener(new ZoomInListener());
        JMenuItem zoomOut=new JMenuItem("Zoom out");
        zoomOut.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_MINUS, InputEvent.CTRL_DOWN_MASK));
        zoomOut.addActionListener(new ZoomOutListener());
        view.add(fit);
        view.add(actual);
        view.addSeparator();
        view.add(zoomIn);
        view.add(zoomOut);
        JMenu help=new JMenu("Help");
        help.setMnemonic(KeyEvent.VK_H);
        JMenuItem docs=new JMenuItem("Documentation");
        docs.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F1, 0));
        docs.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                if (documentationWindow==null||!documentationWindow.isDisplayable()) {
                    documentationWindow=new DocumentationWindow(VectoriumFrame.this);
                }
                documentationWindow.setVisible(true);
                documentationWindow.toFront();
            }
        });
        JMenuItem about=new JMenuItem("About Anlyflad");
        about.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                JOptionPane.showMessageDialog(VectoriumFrame.this,
                    "Anlyflad 1.0.0\n"
                    + "Composition-first raster and SVG vectorization studio.\n\n"
                    + "Author: Richard Jiang\n"
                    + "License: MIT\n\n"
                    + "Raster modes: Color, Binary\n"
                    + "Vector modes: Exact runs, Contour, Curves\n"
                    + "Quality scale: 0 (smallest) to 100 (most detailed)\n"
                    + "Output settings: supersample 0-4, output scale 0.1-16\n\n"
                    + "Workflow: File > Open a PNG, JPEG, or SVG, tune the toolbar and stage inspector, then File > Export SVG.\n\n"
                    + "Runtime: Java " + System.getProperty("java.version") + " on " + System.getProperty("os.name"),
                    "About Anlyflad", JOptionPane.INFORMATION_MESSAGE);
            }
        });
        help.add(docs);
        help.addSeparator();
        help.add(about);
        menuBar.add(file);
        menuBar.add(view);
        menuBar.add(help);
        return menuBar;
    }
    private void updatePipeline() {
        PipelineConfig base=PipelineConfig.defaults().withPreset(toolbar.getPreset()).withRasterMode(toolbar.getRasterMode()).withVectorMode(toolbar.getVectorMode()).withClean(toolbar.isClean());
        int quality=toolbar.getQuality();
        PipelineConfig configured=stageInspector.applyTo(base).withStageValue("vectorize", "mode", toolbar.getVectorMode().getOptionName()).withStageValue("vectorize", "quality", Integer.toString(quality)).withStageValue("quantize", "maxColors", Integer.toString(QualityScale.maxColors(quality)));
        controller.setConfig(configured);
        controller.runPipeline();
    }
    private void updateZoomLabel() {
        toolbar.setZoomLabel(Math.round(canvas.getZoom()*100.0)+"%");
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
    private void schedulePipelineUpdate() {
        pipelineDebounce.restart();
    }
    private final class RunListener implements ActionListener {
        public void actionPerformed(ActionEvent event) {
            pipelineDebounce.stop();
            updatePipeline();
        }
    }
    private final class PresetListener implements ActionListener {
        public void actionPerformed(ActionEvent event) {
            schedulePipelineUpdate();
        }
    }
    private final class ModeListener implements ActionListener {
        public void actionPerformed(ActionEvent event) {
            schedulePipelineUpdate();
        }
    }
    private final class CleanListener implements ActionListener {
        public void actionPerformed(ActionEvent event) {
            schedulePipelineUpdate();
        }
    }
    private final class VectorModeListener implements ActionListener {
        public void actionPerformed(ActionEvent event) {
            schedulePipelineUpdate();
        }
    }
    private final class QualityListener implements ActionListener {
        public void actionPerformed(ActionEvent event) {
            schedulePipelineUpdate();
        }
    }
    private final class FitListener implements ActionListener {
        public void actionPerformed(ActionEvent event) {
            canvas.fitToViewport();
        }
    }
    private final class ActualSizeListener implements ActionListener {
        public void actionPerformed(ActionEvent event) {
            canvas.showActualSize();
        }
    }
    private final class ZoomInListener implements ActionListener {
        public void actionPerformed(ActionEvent event) {
            zoom(ZOOM_STEP);
        }
    }
    private final class ZoomOutListener implements ActionListener {
        public void actionPerformed(ActionEvent event) {
            zoom(1.0/ZOOM_STEP);
        }
    }
    private void zoom(double factor) {
        canvas.zoomAt(canvas.getWidth()/2, canvas.getHeight()/2, factor);
    }
    private static final class WindowCloseListener implements WindowListener {
        private final VectoriumFrame frame;
        private WindowCloseListener(VectoriumFrame frame) {
            this.frame=frame;
        }
        public void windowClosing(WindowEvent event) {
            frame.pipelineDebounce.stop();
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
