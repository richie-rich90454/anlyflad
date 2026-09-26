package com.anlyflad.desktop;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import com.anlyflad.core.model.ParamSpec;
import com.anlyflad.core.model.ParamType;
import com.anlyflad.core.model.StageDescriptor;
import com.anlyflad.core.stage.PipelineConfig;
public final class StageParameterPanel extends JPanel {
    private static final long serialVersionUID=1L;
    private final JLabel title;
    private final JTextArea description;
    private final JPanel fields;
    private final JLabel validation;
    private final Map<String, ParamSpec> parameters;
    private final Map<String, JTextField> textFields;
    private final Map<String, JCheckBox> checkBoxes;
    private StageDescriptor stage;
    private Runnable changeListener;
    private boolean updating;
    public StageParameterPanel() {
        parameters=new LinkedHashMap<String, ParamSpec>();
        textFields=new LinkedHashMap<String, JTextField>();
        checkBoxes=new LinkedHashMap<String, JCheckBox>();
        title=new JLabel("Select a stage");
        title.setFont(DesktopTheme.DISPLAY_FONT);
        title.setForeground(DesktopTheme.TEXT);
        description=new JTextArea(3, 24);
        description.setEditable(false);
        description.setLineWrap(true);
        description.setWrapStyleWord(true);
        description.setOpaque(false);
        description.setFont(DesktopTheme.BODY_FONT);
        description.setForeground(DesktopTheme.MUTED_TEXT);
        fields=new JPanel();
        fields.setLayout(new javax.swing.BoxLayout(fields, javax.swing.BoxLayout.Y_AXIS));
        fields.setOpaque(false);
        validation=new JLabel(" ");
        validation.setFont(DesktopTheme.BODY_FONT);
        validation.setForeground(DesktopTheme.ERROR);
        validation.setBorder(BorderFactory.createEmptyBorder(8, 0, 0, 0));
        validation.getAccessibleContext().setAccessibleName("Parameter validation");
        JPanel header=new JPanel(new BorderLayout(0, 4));
        header.setOpaque(false);
        header.add(title, BorderLayout.NORTH);
        header.add(description, BorderLayout.CENTER);
        JButton resetButton=new JButton("Reset to defaults");
        resetButton.setFont(DesktopTheme.CAPTION_FONT);
        resetButton.setFocusPainted(false);
        resetButton.setBackground(DesktopTheme.SURFACE);
        resetButton.setForeground(DesktopTheme.TEXT);
        resetButton.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(DesktopTheme.BORDER), BorderFactory.createEmptyBorder(5, 10, 5, 10)));
        resetButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent event) {
                resetToDefaults();
            }
        });
        JPanel footer=new JPanel(new BorderLayout(8, 0));
        footer.setOpaque(false);
        footer.add(resetButton, BorderLayout.WEST);
        footer.add(validation, BorderLayout.CENTER);
        JPanel content=new JPanel(new BorderLayout());
        content.setOpaque(false);
        content.add(header, BorderLayout.NORTH);
        content.add(new JScrollPane(fields), BorderLayout.CENTER);
        content.add(footer, BorderLayout.SOUTH);
        setLayout(new BorderLayout());
        setBackground(DesktopTheme.SURFACE);
        setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        add(content, BorderLayout.CENTER);
        setPreferredSize(new Dimension(300, 300));
    }
    public void setStage(StageDescriptor stage, PipelineConfig config) {
        if (config==null) {
            throw new IllegalArgumentException("config must not be null");
        }
        this.stage=stage;
        parameters.clear();
        textFields.clear();
        checkBoxes.clear();
        fields.removeAll();
        if (stage==null) {
            title.setText("Select a stage");
            description.setText("Choose a pipeline stage to edit its parameters.");
            validation.setText(" ");
            repaint();
            return;
        }
        updating=true;
        try {
            title.setText(stage.getLabel());
            description.setText(stage.getDescription());
            if (stage.getParameters().isEmpty()) {
                JLabel empty=new JLabel("This stage has no tunable parameters.");
                empty.setFont(DesktopTheme.BODY_FONT);
                empty.setForeground(DesktopTheme.MUTED_TEXT);
                empty.setBorder(BorderFactory.createEmptyBorder(12, 0, 0, 0));
                fields.add(empty);
            }
            for (int index=0;index<stage.getParameters().size();index++) {
                ParamSpec parameter=stage.getParameters().get(index);
                parameters.put(parameter.getName(), parameter);
                fields.add(createRow(parameter, config.getString(stage.getName(), parameter.getName(), defaultValue(parameter))));
            }
            fields.add(new JPanel());
            validateParameters();
        } finally {
            updating=false;
        }
        fields.revalidate();
        fields.repaint();
    }
    public int getParameterCount() {
        return parameters.size();
    }
    public String getParameterValue(String parameterName) {
        ParamSpec parameter=requireParameter(parameterName);
        JCheckBox checkBox=checkBoxes.get(parameterName);
        if (checkBox!=null) {
            return Boolean.toString(checkBox.isSelected());
        }
        return textFields.get(parameterName).getText();
    }
    public void setParameterValue(String parameterName, String value) {
        ParamSpec parameter=requireParameter(parameterName);
        if (value==null) {
            throw new IllegalArgumentException("value must not be null");
        }
        updating=true;
        try {
            JCheckBox checkBox=checkBoxes.get(parameterName);
            if (checkBox!=null) {
                checkBox.setSelected(Boolean.parseBoolean(value));
            } else {
                textFields.get(parameterName).setText(value);
            }
            validateParameters();
        } finally {
            updating=false;
        }
        notifyChanged();
    }
    public String getValidationMessage() {
        return validation.getText().trim();
    }
    public PipelineConfig applyTo(PipelineConfig config) {
        if (config==null) {
            throw new IllegalArgumentException("config must not be null");
        }
        if (stage==null) {
            return config;
        }
        PipelineConfig updated=config;
        for (int index=0;index<stage.getParameters().size();index++) {
            ParamSpec parameter=stage.getParameters().get(index);
            updated=updated.withStageValue(stage.getName(), parameter.getName(), getParameterValue(parameter.getName()));
        }
        return updated;
    }
    public void setChangeListener(Runnable listener) {
        changeListener=listener;
    }
    private JPanel createRow(ParamSpec parameter, String value) {
        JPanel row=new JPanel(new BorderLayout(8, 0));
        row.setOpaque(false);
        row.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
        JLabel label=new JLabel(parameter.getLabel());
        label.setFont(DesktopTheme.LABEL_FONT);
        label.setForeground(DesktopTheme.TEXT);
        label.setPreferredSize(new Dimension(120, 28));
        row.add(label, BorderLayout.WEST);
        JPanel editorHolder=new JPanel(new BorderLayout(0, 2));
        editorHolder.setOpaque(false);
        if (parameter.getType()==ParamType.BOOLEAN) {
            JCheckBox editor=new JCheckBox();
            editor.setSelected(Boolean.parseBoolean(value));
            editor.setBackground(DesktopTheme.SURFACE);
            editor.setToolTipText(parameter.getDescription());
            editor.getAccessibleContext().setAccessibleName(parameter.getLabel());
            editor.addItemListener(new CheckHandler(parameter.getName()));
            checkBoxes.put(parameter.getName(), editor);
            editorHolder.add(editor, BorderLayout.NORTH);
        } else {
            JTextField editor=new JTextField(value, 12);
            editor.setToolTipText(parameter.getDescription());
            editor.getAccessibleContext().setAccessibleName(parameter.getLabel());
            editor.getDocument().addDocumentListener(new FieldHandler(parameter.getName()));
            textFields.put(parameter.getName(), editor);
            editorHolder.add(editor, BorderLayout.CENTER);
        }
        JLabel hint=new JLabel(hint(parameter));
        hint.setFont(DesktopTheme.CAPTION_FONT);
        hint.setForeground(DesktopTheme.MUTED_TEXT);
        editorHolder.add(hint, BorderLayout.SOUTH);
        row.add(editorHolder, BorderLayout.CENTER);
        return row;
    }
    private static String hint(ParamSpec parameter) {
        StringBuilder hint=new StringBuilder();
        hint.append("Default ").append(parameter.getDefaultValue());
        if (parameter.getMin()!=null||parameter.getMax()!=null) {
            hint.append("  |  ");
            hint.append(parameter.getMin()==null?"-":parameter.getMin().toString());
            hint.append(" to ");
            hint.append(parameter.getMax()==null?"-":parameter.getMax().toString());
        }
        return hint.toString();
    }
    public void resetToDefaults() {
        if (stage==null) {
            return;
        }
        updating=true;
        try {
            for (int index=0;index<stage.getParameters().size();index++) {
                ParamSpec parameter=stage.getParameters().get(index);
                JCheckBox checkBox=checkBoxes.get(parameter.getName());
                if (checkBox!=null) {
                    checkBox.setSelected(Boolean.parseBoolean(parameter.getDefaultValue().toString()));
                } else {
                    textFields.get(parameter.getName()).setText(parameter.getDefaultValue().toString());
                }
            }
            validateParameters();
        } finally {
            updating=false;
        }
        notifyChanged();
    }
    private void validateParameters() {
        if (stage==null) {
            validation.setText(" ");
            return;
        }
        for (int index=0;index<stage.getParameters().size();index++) {
            ParamSpec parameter=stage.getParameters().get(index);
            String value=getParameterValue(parameter.getName());
            if (value.trim().isEmpty()) {
                validation.setText(parameter.getLabel()+" is required");
                return;
            }
            if (parameter.getType()==ParamType.INTEGER||parameter.getType()==ParamType.DOUBLE) {
                double numeric;
                try {
                    numeric=parameter.getType()==ParamType.INTEGER?Integer.parseInt(value):Double.parseDouble(value);
                } catch (NumberFormatException exception) {
                    validation.setText(parameter.getLabel()+" must be numeric");
                    return;
                }
                if (parameter.getType()==ParamType.DOUBLE&&!Double.isFinite(numeric)) {
                    validation.setText(parameter.getLabel()+" must be finite");
                    return;
                }
                if ((parameter.getMin()!=null&&numeric<parameter.getMin().doubleValue())||(parameter.getMax()!=null&&numeric>parameter.getMax().doubleValue())) {
                    validation.setText(parameter.getLabel()+" must be between "+parameter.getMin()+" and "+parameter.getMax());
                    return;
                }
            }
        }
        validation.setText(" ");
    }
    private ParamSpec requireParameter(String parameterName) {
        ParamSpec parameter=parameters.get(parameterName);
        if (parameter==null) {
            throw new IllegalArgumentException("Unknown stage parameter: "+parameterName);
        }
        return parameter;
    }
    private static String defaultValue(ParamSpec parameter) {
        return parameter.getDefaultValue().toString();
    }
    private void notifyChanged() {
        if (changeListener!=null) {
            changeListener.run();
        }
    }
    private final class FieldHandler implements DocumentListener {
        private final String parameterName;
        private FieldHandler(String parameterName) {
            this.parameterName=parameterName;
        }
        public void insertUpdate(DocumentEvent event) {
            changed();
        }
        public void removeUpdate(DocumentEvent event) {
            changed();
        }
        public void changedUpdate(DocumentEvent event) {
            changed();
        }
        private void changed() {
            validateParameters();
            if (!updating) {
                notifyChanged();
            }
        }
    }
    private final class CheckHandler implements ItemListener {
        private final String parameterName;
        private CheckHandler(String parameterName) {
            this.parameterName=parameterName;
        }
        public void itemStateChanged(ItemEvent event) {
            validateParameters();
            if (!updating) {
                notifyChanged();
            }
        }
    }
}
