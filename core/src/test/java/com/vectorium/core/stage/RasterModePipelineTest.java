package com.vectorium.core.stage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import com.vectorium.core.model.RasterOrigin;
import com.vectorium.core.model.VectorDocument;
import com.vectorium.core.model.VectorPath;
import com.vectorium.core.svg.SvgCache;
import com.vectorium.core.svg.SvgParser;
public final class RasterModePipelineTest {
    @Test
    public void shouldPreserveColorsAndSkipDestructiveStages() throws Exception {
        int red=0xFFFF0000;
        int blue=0xFF0000FF;
        int[] pixels={red, red, blue, red, red, blue};
        VectorDocument input=new VectorDocument("raster.png", new RasterOrigin("raster.png"), Collections.<VectorPath>emptyList(), 3, 2, pixels);
        RecordingLogger logger=new RecordingLogger();
        PipelineConfig config=PipelineConfig.defaults();
        Pipeline pipeline=new StageRegistry(logger, new BoundedPipelineMemoizer(), new SvgCache()).buildPipeline(config);
        VectorDocument output=pipeline.run(input, config);
        assertEquals(2, output.getPaths().size());
        assertEquals(red, output.getPaths().get(0).getFill().toArgb());
        assertEquals(blue, output.getPaths().get(1).getFill().toArgb());
        assertEquals(3, output.getWidth());
        assertEquals(2, output.getHeight());
        assertEquals(red, input.getOwnedPixels()[0]);
        assertEquals("validate", logger.name(0));
        assertEquals(StageResult.APPLIED, logger.result(0));
        assertEquals("preprocess", logger.name(1));
        assertEquals(StageResult.SKIPPED, logger.result(1));
        assertEquals("quantize", logger.name(2));
        assertEquals(StageResult.SKIPPED, logger.result(2));
        assertEquals("contour", logger.name(3));
        assertEquals(StageResult.SKIPPED, logger.result(3));
        assertEquals("vectorize", logger.name(4));
        assertEquals(StageResult.APPLIED, logger.result(4));
        assertEquals("serialize", logger.name(14));
        assertEquals(StageResult.APPLIED, logger.result(14));
    }
    @Test
    public void shouldKeepBinaryModeAvailableAndMemoizeByMode() throws Exception {
        int[] pixels={0xFF000000, 0xFFFFFFFF, 0xFFFFFFFF, 0xFF000000};
        VectorDocument input=new VectorDocument("raster.png", new RasterOrigin("raster.png"), Collections.<VectorPath>emptyList(), 2, 2, pixels);
        PipelineConfig color=PipelineConfig.defaults();
        PipelineConfig binary=color.withRasterMode(RasterMode.BINARY);
        assertTrue(!color.equals(binary));
        assertTrue(color.getConfigHash()!=binary.getConfigHash());
        BoundedPipelineMemoizer memoizer=new BoundedPipelineMemoizer();
        Pipeline pipeline=new StageRegistry(new SilentPipelineLogger(), memoizer, new SvgCache()).buildPipeline(color);
        VectorDocument colorOutput=pipeline.run(input, color);
        VectorDocument binaryOutput=pipeline.run(input, binary);
        assertTrue(colorOutput!=binaryOutput);
        assertEquals(2, binaryOutput.getPaths().size());
        assertEquals(0xFF000000, binaryOutput.getPaths().get(0).getFill().toArgb());
    }
    @Test
    public void shouldPassThroughSvgSourceInColorMode() throws Exception {
        String source="<svg width='20' height='10'><rect width='10' height='5' fill='red' stroke='blue'/><text x='1' y='8'>label</text></svg>";
        VectorDocument input=new SvgParser().parse("input.svg", source);
        SvgCache cache=new SvgCache();
        PipelineConfig config=PipelineConfig.defaults();
        Pipeline pipeline=new StageRegistry(new SilentPipelineLogger(), new BoundedPipelineMemoizer(), cache).buildPipeline(config);
        VectorDocument output=pipeline.run(input, config);
        assertSame(input, output);
        assertEquals(source, cache.get(output));
    }
    @Test
    public void shouldRejectDisabledValidation() {
        VectorDocument input=new VectorDocument("raster.png", new RasterOrigin("raster.png"), Collections.<VectorPath>emptyList(), 1, 1, new int[]{0xFFFF0000});
        PipelineConfig config=PipelineConfig.defaults().withStageDisabled("validate");
        Pipeline pipeline=new StageRegistry().buildPipeline(config);
        try {
            pipeline.run(input, config);
            fail("Expected disabled validation to be rejected");
        } catch (StageException exception) {
            assertTrue(exception.getMessage().contains("validate"));
        }
    }
    @Test
    public void shouldRejectDisabledColorVectorization() {
        VectorDocument input=new VectorDocument("raster.png", new RasterOrigin("raster.png"), Collections.<VectorPath>emptyList(), 1, 1, new int[]{0xFFFF0000});
        PipelineConfig config=PipelineConfig.defaults().withStageDisabled("vectorize");
        Pipeline pipeline=new StageRegistry().buildPipeline(config);
        try {
            pipeline.run(input, config);
            fail("Expected disabled color vectorization to be rejected");
        } catch (StageException exception) {
            assertTrue(exception.getMessage().contains("vectorize"));
        }
    }
    private static final class RecordingLogger implements PipelineLogger {
        private final List<String> names=new ArrayList<String>();
        private final List<StageResult> results=new ArrayList<StageResult>();
        public void onStage(String stageName, StageResult result, long nanos) {
            names.add(stageName);
            results.add(result);
        }
        private String name(int index) {
            return names.get(index);
        }
        private StageResult result(int index) {
            return results.get(index);
        }
    }
}
