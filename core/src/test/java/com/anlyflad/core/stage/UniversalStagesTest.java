package com.anlyflad.core.stage;
import java.util.Collections;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import com.anlyflad.core.model.Color;
import com.anlyflad.core.model.PathId;
import com.anlyflad.core.model.RasterOrigin;
import com.anlyflad.core.model.StageDescriptor;
import com.anlyflad.core.model.SvgOrigin;
import com.anlyflad.core.model.VectorDocument;
import com.anlyflad.core.model.VectorPath;
import com.anlyflad.core.svg.SvgCache;
public final class UniversalStagesTest {
    @Test
    public void shouldValidateSvgAndRasterDocuments() throws Exception {
        StageDescriptor descriptor=new StageDescriptor("validate", "Validate", "Validates", true);
        ValidateStage stage=new ValidateStage(descriptor);
        VectorDocument svg=svg();
        VectorDocument raster=new VectorDocument("raster", new RasterOrigin("raster"), Collections.<VectorPath>emptyList(), 1, 1, new int[1]);
        assertSame(svg, stage.apply(svg));
        assertSame(raster, stage.apply(raster));
        try {
            stage.apply(null);
            fail("Expected null document to be rejected");
        } catch (StageException exception) {
            assertTrue(exception.getMessage().length()>0);
        }
        try {
            stage.apply(VectorDocument.emptyRaster("empty", 0, 0));
            fail("Expected zero-sized raster to be rejected");
        } catch (StageException exception) {
            assertTrue(exception.getMessage().length()>0);
        }
    }
    @Test
    public void shouldNormalizeGeometryToNonnegativeOrigin() throws Exception {
        StageDescriptor descriptor=new StageDescriptor("normalize", "Normalize", "Normalizes", true);
        NormalizeStage stage=new NormalizeStage(descriptor, true);
        VectorPath path=new VectorPath(PathId.zero(), new double[]{-3.0, -2.0, 2.0, -2.0, 2.0, 3.0, -3.0, 3.0}, true, new Color(0, 0, 0, 255), 1.0);
        VectorDocument input=new VectorDocument("normalize", new SvgOrigin("normalize"), Collections.singletonList(path), 10, 10, new int[100]);
        VectorDocument output=stage.apply(input);
        assertEquals(0.0, output.getPaths().get(0).getBounds().getMinX(), 0.0);
        assertEquals(0.0, output.getPaths().get(0).getBounds().getMinY(), 0.0);
        assertEquals(5, output.getWidth());
        assertEquals(5, output.getHeight());
        NormalizeStage disabled=(NormalizeStage)stage.withConfig(PipelineConfig.defaults().withStageValue("normalize", "enabled", "false"));
        assertSame(input, disabled.apply(input));
    }
    @Test
    public void shouldSerializeThroughIdentityCacheWithoutChangingDocument() throws Exception {
        StageDescriptor descriptor=new StageDescriptor("serialize", "Serialize", "Serializes", true);
        SvgCache cache=new SvgCache();
        SerializeStage stage=new SerializeStage(descriptor, cache);
        VectorDocument document=svg();
        assertSame(document, stage.apply(document));
        assertEquals(1, cache.size());
        assertTrue(stage.appliesTo(document));
        assertFalse(stage.appliesTo(null));
    }
    private VectorDocument svg() {
        VectorPath path=new VectorPath(PathId.zero(), new double[]{0.0, 0.0, 1.0, 0.0, 1.0, 1.0}, true, new Color(0, 0, 0, 255), 1.0);
        return new VectorDocument("svg", new SvgOrigin("svg"), Collections.singletonList(path), 2, 2, new int[4]);
    }
}
