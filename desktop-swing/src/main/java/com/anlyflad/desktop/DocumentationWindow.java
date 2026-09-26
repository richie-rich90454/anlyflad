package com.anlyflad.desktop;
import java.awt.BorderLayout;
import java.awt.Desktop;
import java.awt.Dimension;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.ListSelectionModel;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import com.anlyflad.core.docs.MarkdownRenderer;
public final class DocumentationWindow extends JFrame {
    private static final long serialVersionUID=1L;
    private static final String[] TITLES={"README", "Architecture", "Performance", "TeaVM"};
    private static final String[] PATHS={"/docs/README.md", "/docs/architecture.md", "/docs/performance.md", "/docs/teavm.md"};
    private final JList<String> documentList;
    private final JEditorPane viewer;
    public DocumentationWindow(JFrame owner) {
        super("Anlyflad Documentation");
        documentList=new JList<String>(new DefaultListModel<String>());
        DefaultListModel<String> model=(DefaultListModel<String>)documentList.getModel();
        for (int index=0;index<TITLES.length;index++) {
            model.addElement(TITLES[index]);
        }
        documentList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        documentList.setFont(DesktopTheme.BODY_FONT);
        documentList.setBackground(DesktopTheme.SURFACE_ALT);
        documentList.setFixedCellHeight(30);
        documentList.getAccessibleContext().setAccessibleName("Documentation pages");
        viewer=new JEditorPane();
        viewer.setEditable(false);
        viewer.setContentType("text/html");
        viewer.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, Boolean.TRUE);
        viewer.setFont(DesktopTheme.BODY_FONT);
        viewer.setBackground(DesktopTheme.PAGE);
        viewer.getAccessibleContext().setAccessibleName("Documentation content");
        viewer.addHyperlinkListener(new DocumentationLinkHandler());
        JScrollPane listScroll=new JScrollPane(documentList);
        listScroll.setBorder(BorderFactory.createEmptyBorder());
        JScrollPane viewerScroll=new JScrollPane(viewer);
        viewerScroll.setBorder(BorderFactory.createEmptyBorder());
        JSplitPane split=new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, listScroll, viewerScroll);
        split.setResizeWeight(0.22);
        split.setDividerLocation(190);
        split.setContinuousLayout(true);
        split.setBorder(null);
        setLayout(new BorderLayout());
        add(split, BorderLayout.CENTER);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(960, 720);
        setMinimumSize(new Dimension(640, 480));
        setLocationRelativeTo(owner);
        documentList.addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent event) {
                if (!event.getValueIsAdjusting()) {
                    showSelected();
                }
            }
        });
        documentList.setSelectedIndex(0);
    }
    public void showDocument(String title) {
        for (int index=0;index<TITLES.length;index++) {
            if (TITLES[index].equalsIgnoreCase(title)) {
                documentList.setSelectedIndex(index);
                return;
            }
        }
    }
    private void showSelected() {
        int index=documentList.getSelectedIndex();
        if (index<0||index>=PATHS.length) {
            return;
        }
        String markdown=readResource(PATHS[index]);
        if (markdown==null) {
            viewer.setText("<html><body><p>Documentation is not bundled in this build.</p></body></html>");
            return;
        }
        viewer.setText(html(markdown));
        viewer.setCaretPosition(0);
    }
    private String html(String markdown) {
        return "<html><head><style>"
            + "body{font-family:'Noto Sans',sans-serif;font-size:13px;color:#25231f;background:#fffcf6;margin:18px;line-height:1.6}"
            + "h1{font-size:24px;color:#25231f;border-bottom:1px solid #b8b1a4;padding-bottom:6px}"
            + "h2{font-size:19px;color:#25231f;margin-top:22px}"
            + "h3{font-size:16px;color:#25231f;margin-top:18px}"
            + "p{margin:8px 0}"
            + "a{color:#d65a31}"
            + "code{font-family:'Noto Sans',sans-serif;background:#f3f0e8;border:1px solid #e3ddd0;padding:1px 4px}"
            + "pre{font-family:'Noto Sans',sans-serif;background:#f3f0e8;border:1px solid #b8b1a4;padding:10px;white-space:pre-wrap}"
            + "table{border-collapse:collapse;margin:10px 0}"
            + "th,td{border:1px solid #b8b1a4;padding:5px 9px;text-align:left}"
            + "th{background:#f3f0e8}"
            + "blockquote{border-left:3px solid #d65a31;margin:8px 0;padding:2px 12px;color:#6f6a61}"
            + "hr{border:0;border-top:1px solid #b8b1a4}"
            + "</style></head><body>"
            + MarkdownRenderer.render(markdown)
            + "</body></html>";
    }
    private static String readResource(String path) {
        InputStream stream=DocumentationWindow.class.getResourceAsStream(path);
        if (stream==null) {
            return null;
        }
        try {
            ByteArrayOutputStream output=new ByteArrayOutputStream();
            byte[] buffer=new byte[8192];
            int read;
            while ((read=stream.read(buffer))>=0) {
                output.write(buffer,0,read);
            }
            return new String(output.toByteArray(), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            return null;
        } finally {
            try {
                stream.close();
            } catch (IOException exception) {
            }
        }
    }
    private final class DocumentationLinkHandler implements HyperlinkListener {
        public void hyperlinkUpdate(HyperlinkEvent event) {
            if (event.getEventType()!=HyperlinkEvent.EventType.ACTIVATED) {
                return;
            }
            String description=event.getDescription();
            if (description!=null&&description.toLowerCase(java.util.Locale.ROOT).endsWith(".md")) {
                int slash=description.lastIndexOf('/');
                String name=slash>=0?description.substring(slash+1):description;
                int dot=name.lastIndexOf('.');
                showDocument(dot>0?name.substring(0,dot):name);
                return;
            }
            try {
                if (Desktop.isDesktopSupported()&&event.getURL()!=null) {
                    Desktop.getDesktop().browse(event.getURL().toURI());
                }
            } catch (Exception exception) {
            }
        }
    }
}
