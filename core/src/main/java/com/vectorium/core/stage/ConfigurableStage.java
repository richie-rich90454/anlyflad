package com.vectorium.core.stage;
public interface ConfigurableStage extends Stage {
    Stage withConfig(PipelineConfig config);
}
