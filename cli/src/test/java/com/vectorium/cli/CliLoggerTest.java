package com.vectorium.cli;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertTrue;
import com.vectorium.core.stage.StageResult;
public final class CliLoggerTest {
    @Test
    public void shouldWriteMachineReadableStageReport() {
        ByteArrayOutputStream bytes=new ByteArrayOutputStream();
        CliLogger logger=new CliLogger(new PrintStream(bytes));
        logger.onStage("simplify", StageResult.APPLIED, 1500000L);
        logger.onStage("smooth", StageResult.SKIPPED, 0L);
        String report=bytes.toString();
        assertTrue(report.contains("simplify\tAPPLIED\t1500000 ns"));
        assertTrue(report.contains("smooth\tSKIPPED\t0 ns"));
    }
}
