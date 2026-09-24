package com.vectorium.core.stage;
public interface PipelineLogger {
    void onStage(String stageName, StageResult result, long nanos);
}
