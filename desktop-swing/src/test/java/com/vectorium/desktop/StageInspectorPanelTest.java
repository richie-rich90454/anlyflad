package com.vectorium.desktop;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import com.vectorium.core.stage.PipelineConfig;
import com.vectorium.core.stage.StageRegistry;
public final class StageInspectorPanelTest {
    @Test
    public void shouldListSelectAndConfigureStages() {
        StageInspectorPanel panel=new StageInspectorPanel(new StageRegistry(), PipelineConfig.defaults());
        assertEquals(15, panel.getStageCount());
        assertEquals("validate", panel.getSelectedStageName());
        panel.selectStage("smooth");
        assertEquals("smooth", panel.getSelectedStageName());
        assertEquals(1, panel.getParameterPanel().getParameterCount());
        assertEquals("1", panel.getParameterPanel().getParameterValue("passes"));
        panel.getParameterPanel().setParameterValue("passes", "2");
        PipelineConfig config=panel.applyTo(PipelineConfig.defaults());
        assertEquals(2, config.getInteger("smooth", "passes", -1));
        panel.setStageEnabled("smooth", false);
        assertFalse(panel.applyTo(PipelineConfig.defaults()).isStageEnabled("smooth"));
    }
    @Test
    public void shouldValidateParameterInputInline() {
        StageInspectorPanel panel=new StageInspectorPanel(new StageRegistry(), PipelineConfig.defaults());
        panel.selectStage("smooth");
        panel.getParameterPanel().setParameterValue("passes", "4");
        assertTrue(panel.getParameterPanel().getValidationMessage().contains("between"));
        panel.getParameterPanel().setParameterValue("passes", "1");
        assertTrue(panel.getParameterPanel().getValidationMessage().isEmpty());
    }
}
