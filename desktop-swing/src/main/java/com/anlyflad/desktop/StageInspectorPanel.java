package com.anlyflad.desktop;
import java.awt.BorderLayout;
import java.awt.Dimension;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import com.anlyflad.core.stage.PipelineConfig;
import com.anlyflad.core.stage.StageRegistry;
public final class StageInspectorPanel extends JPanel {
    private static final long serialVersionUID=1L;
    private final StageListPanel stageList;
    private final StageParameterPanel parameterPanel;
    private PipelineConfig currentConfig;
    public StageInspectorPanel(StageRegistry registry, PipelineConfig config) {
        if (registry==null||config==null) {
            throw new IllegalArgumentException("registry and config must not be null");
        }
        currentConfig=config;
        stageList=new StageListPanel(registry, config);
        parameterPanel=new StageParameterPanel();
        stageList.setSelectionListener(new Runnable() {
            public void run() {
                parameterPanel.setStage(stageList.getSelectedStage(), currentConfig);
            }
        });
        JLabel eyebrow=new JLabel("PIPELINE");
        eyebrow.setFont(DesktopTheme.CAPTION_FONT);
        eyebrow.setForeground(DesktopTheme.ACCENT);
        JLabel title=new JLabel("Stages");
        title.setFont(DesktopTheme.TITLE_FONT);
        title.setForeground(DesktopTheme.TEXT);
        JLabel detail=new JLabel("Select a stage to tune its parameters.");
        detail.setFont(DesktopTheme.BODY_FONT);
        detail.setForeground(DesktopTheme.MUTED_TEXT);
        JPanel heading=new JPanel(new BorderLayout(0, 2));
        heading.setOpaque(false);
        heading.setBorder(BorderFactory.createEmptyBorder(16, 16, 12, 16));
        heading.add(eyebrow, BorderLayout.NORTH);
        JPanel titles=new JPanel(new BorderLayout());
        titles.setOpaque(false);
        titles.add(title, BorderLayout.NORTH);
        titles.add(detail, BorderLayout.SOUTH);
        heading.add(titles, BorderLayout.CENTER);
        JSplitPane split=new JSplitPane(JSplitPane.VERTICAL_SPLIT, stageList, parameterPanel);
        split.setResizeWeight(0.5);
        split.setDividerLocation(300);
        split.setContinuousLayout(true);
        split.setOneTouchExpandable(true);
        split.setBorder(null);
        JPanel content=new JPanel(new BorderLayout());
        content.setOpaque(false);
        content.add(heading, BorderLayout.NORTH);
        content.add(split, BorderLayout.CENTER);
        setLayout(new BorderLayout());
        setBackground(DesktopTheme.SURFACE);
        setBorder(BorderFactory.createMatteBorder(0, 1, 0, 0, DesktopTheme.BORDER));
        setPreferredSize(new Dimension(420, 600));
        add(content, BorderLayout.CENTER);
        parameterPanel.setStage(stageList.getSelectedStage(), config);
    }
    public int getStageCount() {
        return stageList.getStageCount();
    }
    public void selectStage(String stageName) {
        stageList.selectStage(stageName);
    }
    public String getSelectedStageName() {
        return stageList.getSelectedStageName();
    }
    public StageParameterPanel getParameterPanel() {
        return parameterPanel;
    }
    public void setStageEnabled(String stageName, boolean enabled) {
        stageList.setStageEnabled(stageName, enabled);
    }
    public boolean isStageEnabled(String stageName) {
        return stageList.isStageEnabled(stageName);
    }
    public PipelineConfig applyTo(PipelineConfig config) {
        return stageList.applyTo(parameterPanel.applyTo(config));
    }
    public void setConfig(PipelineConfig config) {
        if (config==null) {
            throw new IllegalArgumentException("config must not be null");
        }
        currentConfig=config;
        stageList.setConfig(config);
        parameterPanel.setStage(stageList.getSelectedStage(), applyTo(config));
    }
    public void setChangeListener(Runnable listener) {
        stageList.setChangeListener(listener);
        parameterPanel.setChangeListener(listener);
    }
}
