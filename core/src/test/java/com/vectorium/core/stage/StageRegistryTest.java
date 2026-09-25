package com.vectorium.core.stage;
import java.util.Arrays;
import java.util.Collections;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import com.vectorium.core.model.Color;
import com.vectorium.core.model.PathId;
import com.vectorium.core.model.SvgOrigin;
import com.vectorium.core.model.VectorDocument;
import com.vectorium.core.model.VectorPath;
import com.vectorium.core.raster.AdaptiveColorQuantizer;
public final class StageRegistryTest {
    @Test
    public void shouldExposeAllStagesInDefaultOrder() {
        StageRegistry registry=new StageRegistry();
        String[] expected={"validate", "preprocess", "quantize", "contour", "vectorize", "speck-filter", "color-merge", "union", "simplify", "smooth", "hole-fix", "layer-sort", "dedupe", "normalize", "serialize"};
        assertEquals(expected.length, registry.names().size());
        for (int index=0;index<expected.length;index++) {
            assertEquals(expected[index], registry.names().get(index));
            Stage stage=registry.create(expected[index]);
            assertEquals(expected[index], stage.getName());
            assertEquals(expected[index], registry.getDescriptor(expected[index]).getName());
        }
        Pipeline pipeline=registry.buildDefaultPipeline();
        assertEquals(expected.length, pipeline.getStages().size());
        try {
            registry.names().clear();
            fail("Expected stage names to be unmodifiable");
        } catch (UnsupportedOperationException exception) {
            assertEquals(UnsupportedOperationException.class, exception.getClass());
        }
    }
    @Test
    public void shouldExposeAdaptiveColorLimit() {
        assertEquals(2, StandardStageDescriptors.QUANTIZE.getParameters().size());
        assertEquals("maxColors", StandardStageDescriptors.QUANTIZE.getParameters().get(1).getName());
        assertEquals(Integer.valueOf(AdaptiveColorQuantizer.DEFAULT_MAX_COLORS), StandardStageDescriptors.QUANTIZE.getParameters().get(1).getDefaultValue());
    }
    @Test
    public void shouldCreateRunnablePipelineAndApplyParameterOverrides() throws Exception {
        StageRegistry registry=new StageRegistry();
        PipelineConfig config=PipelineConfig.defaults().withStageValue("normalize", "enabled", "false");
        Pipeline pipeline=registry.buildPipeline(config);
        VectorPath path=new VectorPath(PathId.zero(), new double[]{-1.0, -1.0, 1.0, -1.0, 1.0, 1.0, -1.0, 1.0}, true, new Color(0, 0, 0, 255), 1.0);
        VectorDocument input=new VectorDocument("registry", new SvgOrigin("registry"), Collections.singletonList(path), 4, 4, new int[16]);
        VectorDocument output=pipeline.run(input, config);
        assertEquals(4, output.getWidth());
        assertEquals(-1.0, output.getPaths().get(0).getBounds().getMinX(), 0.0);
    }
    @Test
    public void shouldComparePathsByAreaAndId() {
        VectorPath small=rectangle(1, 0.0, 0.0, 1.0, 1.0);
        VectorPath large=rectangle(0, 0.0, 0.0, 2.0, 2.0);
        VectorPathAreaComparator descending=new VectorPathAreaComparator(true);
        assertTrue(descending.compare(large, small)<0);
        assertTrue(descending.compare(small, large)>0);
        VectorPathAreaComparator ascending=new VectorPathAreaComparator(false);
        assertTrue(ascending.compare(small, large)<0);
        assertEquals(0, descending.compare(large, large));
    }
    @Test
    public void shouldRejectUnknownStageNames() {
        StageRegistry registry=new StageRegistry();
        try {
            registry.create("missing");
            fail("Expected unknown stage to be rejected");
        } catch (IllegalArgumentException exception) {
            assertTrue(exception.getMessage().length()>0);
        }
    }
    private VectorPath rectangle(int id, double minX, double minY, double maxX, double maxY) {
        return new VectorPath(PathId.of(id), new double[]{minX, minY, maxX, minY, maxX, maxY, minX, maxY}, true, new Color(0, 0, 0, 255), 1.0);
    }
}
