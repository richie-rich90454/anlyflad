package com.anlyflad.cli;
import java.io.PrintStream;
import com.anlyflad.core.stage.PipelineLogger;
import com.anlyflad.core.stage.StageResult;
public final class CliLogger implements PipelineLogger {
    private final PrintStream output;
    public CliLogger(PrintStream output) {
        if (output==null) {
            throw new IllegalArgumentException("output must not be null");
        }
        this.output=output;
    }
    public void onStage(String stageName, StageResult result, long nanos) {
        if (stageName==null||stageName.trim().isEmpty()) {
            throw new IllegalArgumentException("stageName must not be blank");
        }
        if (result==null) {
            throw new IllegalArgumentException("result must not be null");
        }
        if (nanos<0L) {
            throw new IllegalArgumentException("nanos must be nonnegative");
        }
        output.println(stageName+"\t"+result.name()+"\t"+nanos+" ns");
    }
}
