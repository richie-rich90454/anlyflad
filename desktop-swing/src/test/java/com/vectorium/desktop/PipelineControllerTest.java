package com.vectorium.desktop;
import java.awt.image.BufferedImage;
import java.io.File;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import javax.imageio.ImageIO;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import com.vectorium.core.stage.PipelineConfig;
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
        controller.setAutoRun(false);
        final CountDownLatch completed=new CountDownLatch(2);
        controller.setDocumentListener(new Runnable() {
            public void run() {
                completed.countDown();
            }
        });
        try {
            controller.load(input);
            controller.awaitIdle();
            controller.runPipeline();
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
    public void shouldResetProgressForEachPipelineRun() throws Exception {
        File input=new File(temporaryDirectory.toFile(), "repeat.png");
        BufferedImage image=new BufferedImage(2, 2, BufferedImage.TYPE_INT_ARGB);
        image.setRGB(0, 0, 0xFFFF0000);
        image.setRGB(1, 0, 0xFF0000FF);
        image.setRGB(0, 1, 0xFFFF0000);
        image.setRGB(1, 1, 0xFF0000FF);
        assertTrue(ImageIO.write(image, "png", input));
        VectorCanvas canvas=new VectorCanvas();
        StatusBar status=new StatusBar();
        PipelineController controller=new PipelineController(canvas, status);
        controller.setAutoRun(false);
        try {
            controller.load(input);
            controller.awaitIdle();
            assertNull(controller.getResult());
            controller.runPipeline();
            controller.awaitIdle();
            assertEquals(15, completedStages(controller));
            controller.setConfig(PipelineConfig.defaults().withPreset("repeat"));
            controller.runPipeline();
            controller.awaitIdle();
            SwingUtilities.invokeAndWait(new Runnable() {
                public void run() {
                }
            });
            assertEquals(15, status.getProgressValue());
            assertEquals(15, status.getProgressMaximum());
            assertEquals(15, completedStages(controller));
        } finally {
            controller.close();
        }
    }
    @Test
    public void shouldAwaitAutomaticRun() throws Exception {
        File input=new File(temporaryDirectory.toFile(), "auto.svg");
        Files.write(input.toPath(), "<svg width='10' height='10'><path d='M0 0 L1 1'/></svg>".getBytes(StandardCharsets.UTF_8));
        VectorCanvas canvas=new VectorCanvas();
        StatusBar status=new StatusBar();
        PipelineController controller=new PipelineController(canvas, status);
        try {
            controller.load(input);
            controller.awaitIdle();
            assertNotNull(controller.getResult());
            assertEquals("Vectorization complete", status.getStatusText());
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
    private static int completedStages(PipelineController controller) throws Exception {
        Field loggerField=PipelineController.class.getDeclaredField("uiLogger");
        loggerField.setAccessible(true);
        Object logger=loggerField.get(controller);
        Field completedField=logger.getClass().getDeclaredField("completed");
        completedField.setAccessible(true);
        return completedField.getInt(logger);
    }
}
