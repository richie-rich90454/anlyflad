package com.anlyflad.desktop;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import com.anlyflad.core.model.StageDescriptor;
import com.anlyflad.core.stage.PipelineConfig;
import com.anlyflad.core.stage.StageRegistry;
public final class StageListPanel extends JPanel {
    private static final long serialVersionUID=1L;
    private final StageRegistry registry;
    private final List<String> names;
    private final Map<String, Boolean> enabled;
    private final JList<String> stageList;
    private final JCheckBox enabledCheck;
    private final JTextArea description;
    private Runnable selectionListener;
    private Runnable changeListener;
    private boolean updating;
    public StageListPanel(StageRegistry registry, PipelineConfig config) {
        if (registry==null||config==null) {
            throw new IllegalArgumentException("registry and config must not be null");
        }
        this.registry=registry;
        names=Collections.unmodifiableList(new ArrayList<String>(registry.names()));
        enabled=new LinkedHashMap<String, Boolean>();
        DefaultListModel<String> model=new DefaultListModel<String>();
        for (int index=0;index<names.size();index++) {
            StageDescriptor descriptor=registry.getDescriptor(names.get(index));
            model.addElement(descriptor.getLabel());
            enabled.put(names.get(index), Boolean.valueOf(config.isStageEnabled(names.get(index))));
        }
        stageList=new JList<String>(model);
        stageList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        stageList.setFont(DesktopTheme.BODY_FONT);
        stageList.setBackground(DesktopTheme.SURFACE_ALT);
        stageList.setFixedCellHeight(30);
        stageList.setCellRenderer(new StageRenderer());
        stageList.getAccessibleContext().setAccessibleName("Pipeline stages");
        enabledCheck=new JCheckBox("Stage enabled");
        enabledCheck.setFont(DesktopTheme.LABEL_FONT);
        enabledCheck.setBackground(DesktopTheme.SURFACE);
        enabledCheck.getAccessibleContext().setAccessibleName("Enable selected stage");
        description=new JTextArea(4, 20);
        description.setEditable(false);
        description.setLineWrap(true);
        description.setWrapStyleWord(true);
        description.setOpaque(false);
        description.setFont(DesktopTheme.BODY_FONT);
        description.setForeground(DesktopTheme.MUTED_TEXT);
        description.setBorder(BorderFactory.createEmptyBorder(8, 0, 0, 0));
        description.getAccessibleContext().setAccessibleName("Stage description");
        JPanel controls=new JPanel(new BorderLayout());
        controls.setOpaque(false);
        controls.add(enabledCheck, BorderLayout.NORTH);
        controls.add(description, BorderLayout.CENTER);
        setLayout(new BorderLayout());
        setBackground(DesktopTheme.SURFACE);
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        add(new JScrollPane(stageList), BorderLayout.CENTER);
        add(controls, BorderLayout.SOUTH);
        setPreferredSize(new Dimension(220, 300));
        stageList.addListSelectionListener(new SelectionHandler());
        enabledCheck.addItemListener(new EnabledHandler());
        stageList.setSelectedIndex(0);
        updateSelectedStage();
    }
    public int getStageCount() {
        return names.size();
    }
    public List<String> getStageNames() {
        return names;
    }
    public void selectStage(String stageName) {
        int index=names.indexOf(stageName);
        if (index<0) {
            throw new IllegalArgumentException("Unknown stage: "+stageName);
        }
        stageList.setSelectedIndex(index);
    }
    public String getSelectedStageName() {
        return names.get(stageList.getSelectedIndex());
    }
    public StageDescriptor getSelectedStage() {
        return registry.getDescriptor(getSelectedStageName());
    }
    public void setStageEnabled(String stageName, boolean value) {
        ensureMutableStage(stageName);
        enabled.put(stageName, Boolean.valueOf(value));
        if (stageName.equals(getSelectedStageName())) {
            updating=true;
            try {
                enabledCheck.setSelected(value);
                enabledCheck.setEnabled(true);
            } finally {
                updating=false;
            }
        }
        notifyChanged();
    }
    public boolean isStageEnabled(String stageName) {
        ensureKnownStage(stageName);
        return enabled.get(stageName).booleanValue();
    }
    public PipelineConfig applyTo(PipelineConfig config) {
        if (config==null) {
            throw new IllegalArgumentException("config must not be null");
        }
        PipelineConfig updated=config;
        for (int index=0;index<names.size();index++) {
            String name=names.get(index);
            if (!enabled.get(name).booleanValue()) {
                updated=updated.withStageDisabled(name);
            }
        }
        return updated;
    }
    public void setConfig(PipelineConfig config) {
        if (config==null) {
            throw new IllegalArgumentException("config must not be null");
        }
        updating=true;
        try {
            for (int index=0;index<names.size();index++) {
                String name=names.get(index);
                enabled.put(name, Boolean.valueOf(isProtectedStage(name)||config.isStageEnabled(name)));
            }
            updateSelectedStage();
        } finally {
            updating=false;
        }
    }
    public void setSelectionListener(Runnable listener) {
        selectionListener=listener;
    }
    public void setChangeListener(Runnable listener) {
        changeListener=listener;
    }
    private void updateSelectedStage() {
        if (stageList.getSelectedIndex()<0) {
            return;
        }
        String stageName=getSelectedStageName();
        boolean selected=enabled.get(stageName).booleanValue();
        enabledCheck.setSelected(selected);
        enabledCheck.setEnabled(!isProtectedStage(stageName));
        description.setText(registry.getDescriptor(stageName).getDescription());
    }
    private void notifyChanged() {
        if (!updating&&changeListener!=null) {
            changeListener.run();
        }
    }
    private void ensureKnownStage(String stageName) {
        if (!names.contains(stageName)) {
            throw new IllegalArgumentException("Unknown stage: "+stageName);
        }
    }
    private void ensureMutableStage(String stageName) {
        ensureKnownStage(stageName);
        if (isProtectedStage(stageName)) {
            throw new IllegalArgumentException("The validate, serialize, and vectorize stages cannot be disabled");
        }
    }
    private boolean isProtectedStage(String stageName) {
        return "validate".equals(stageName)||"serialize".equals(stageName)||"vectorize".equals(stageName);
    }
    private static final class StageRenderer extends javax.swing.DefaultListCellRenderer {
        private static final long serialVersionUID=1L;
        public java.awt.Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean selected, boolean focused) {
            JLabel label=(JLabel)super.getListCellRendererComponent(list, value, index, selected, focused);
            label.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
            label.setFont(DesktopTheme.BODY_FONT);
            if (selected) {
                label.setBackground(DesktopTheme.ACCENT_SOFT);
                label.setForeground(DesktopTheme.TEXT);
            } else {
                label.setBackground(index%2==0?DesktopTheme.SURFACE:DesktopTheme.SURFACE_ALT);
                label.setForeground(DesktopTheme.TEXT);
            }
            return label;
        }
    }
    private final class SelectionHandler implements ListSelectionListener {
        public void valueChanged(ListSelectionEvent event) {
            if (!event.getValueIsAdjusting()) {
                updateSelectedStage();
                if (selectionListener!=null) {
                    selectionListener.run();
                }
            }
        }
    }
    private final class EnabledHandler implements ItemListener {
        public void itemStateChanged(ItemEvent event) {
            if (!updating) {
                enabled.put(getSelectedStageName(), Boolean.valueOf(enabledCheck.isSelected()));
                notifyChanged();
            }
        }
    }
}
