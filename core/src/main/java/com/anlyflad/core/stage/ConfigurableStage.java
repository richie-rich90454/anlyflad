package com.anlyflad.core.stage;
public interface ConfigurableStage extends Stage {
    Stage withConfig(PipelineConfig config);
}
