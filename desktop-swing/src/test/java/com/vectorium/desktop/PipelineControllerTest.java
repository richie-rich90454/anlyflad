package com.vectorium.desktop;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
public final class PipelineControllerTest {
    @TempDir
    public Path temporaryDirectory;
    @Test
    public void shouldLoadRunAndExportWithBoundedShutdown() throws Exception {
        File input=new File(temporaryDirectory.toFile(), "input.svg");
        File output=new File(temporaryDirectory.toFile(), "output.svg");
        Files.write(input.toPath(), "<svg width='20' height='10'><path d='M0 0 L10 0 L10 10 Z'/></svg>".getBytes(StandardCharsets.UTF_8));
        VectorCanvas canvas=new VectorCanvas();
        StatusBar status=new StatusBar();
        PipelineController controller=new PipelineController(canvas, status);
        final CountDownLatch completed=new CountDownLatch(2);
        controller.setDocumentListener(new Runnable() {
            public void run() {
                completed.countDown();
            }
        });
        try {
            controller.load(input);
            controller.awaitIdle();
            assertTrue(completed.await(5L, TimeUnit.SECONDS));
            controller.export(output);
            controller.awaitIdle();
            assertTrue(output.isFile());
            assertTrue(new String(Files.readAllBytes(output.toPath()), StandardCharsets.UTF_8).startsWith("<svg"));
            assertEquals(1, controller.getResult().getPaths().size());
        } finally {
            controller.close();
        }
    }
    @Test
    public void shouldFlushUiUpdatesBeforeReturning() throws Exception {
        VectorCanvas canvas=new VectorCanvas();
        StatusBar status=new StatusBar();
        PipelineController controller=new PipelineController(canvas, status);
        try {
            controller.load(new File(temporaryDirectory.toFile(), "missing.svg"));
            controller.awaitIdle();
            SwingUtilities.invokeAndWait(new Runnable() {
                public void run() {
                }
            });
            assertTrue(status.getStatusText().startsWith("Error:"));
        } finally {
            controller.close();
        }
    }
}
