package com.vectorium.core.stage;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import com.vectorium.core.model.Color;
import com.vectorium.core.model.RasterOrigin;
import com.vectorium.core.model.StageDescriptor;
import com.vectorium.core.model.SvgOrigin;
import com.vectorium.core.model.VectorDocument;
public final class RasterStagesTest {
    @Test
    public void shouldPreprocessCopiedPixelsAndApplyOverrides() throws Exception {
        StageDescriptor descriptor=new StageDescriptor("preprocess", "Preprocess", "Prepares pixels", true);
        PreprocessStage stage=new PreprocessStage(descriptor, true, 0, 1.0);
        int[] originalPixels={0xFF204060, 0xFF806020};
        VectorDocument input=raster(originalPixels);
        VectorDocument output=stage.apply(input);
        assertEquals(0x003A3A3A, output.getOwnedPixels()[0]&0xFFFFFF);
        assertEquals(0x00626262, output.getOwnedPixels()[1]&0xFFFFFF);
        assertEquals(0x00204060, input.getOwnedPixels()[0]&0xFFFFFF);
        Stage configured=(Stage)stage.withConfig(PipelineConfig.defaults().withStageValue("preprocess", "brightness", "10"));
        VectorDocument brightened=configured.apply(input);
        assertEquals(0x00444444, brightened.getOwnedPixels()[0]&0xFFFFFF);
    }
    @Test
    public void shouldQuantizeCopiedPixelsAndPreserveAlpha() throws Exception {
        StageDescriptor descriptor=new StageDescriptor("quantize", "Quantize", "Maps colors", true);
        int[] palette={0x000000, 0xFFFFFF};
        QuantizeStage stage=new QuantizeStage(descriptor, palette);
        palette[0]=0xFF0000;
        VectorDocument input=raster(new int[]{0x80101010, 0x80F0F0F0});
        VectorDocument output=stage.apply(input);
        assertEquals(0x80000000, output.getOwnedPixels()[0]);
        assertEquals(0x80FFFFFF, output.getOwnedPixels()[1]);
        assertEquals(0x80101010, input.getOwnedPixels()[0]);
        Stage configured=stage.withConfig(PipelineConfig.defaults().withStageValue("quantize", "palette", "16711680,0"));
        VectorDocument redInput=raster(new int[]{0x80F02020, 0x80F0F0F0});
        VectorDocument red=configured.apply(redInput);
        assertEquals(0x80FF0000, red.getOwnedPixels()[0]);
    }
    @Test
    public void shouldThresholdAndVectorizeForegroundPixels() throws Exception {
        StageDescriptor contourDescriptor=new StageDescriptor("contour", "Contour", "Thresholds pixels", true);
        ContourStage contour=new ContourStage(contourDescriptor, 128);
        VectorDocument thresholded=contour.apply(raster(new int[]{0xFF000000, 0xFFFFFFFF, 0xFF000000, 0xFFFFFFFF}));
        assertEquals(0xFF000000, thresholded.getOwnedPixels()[0]);
        assertEquals(0xFFFFFFFF, thresholded.getOwnedPixels()[1]);
        StageDescriptor vectorizeDescriptor=new StageDescriptor("vectorize", "Vectorize", "Creates paths", true);
        VectorizeStage vectorize=new VectorizeStage(vectorizeDescriptor);
        VectorDocument output=vectorize.apply(thresholded);
        assertEquals(1, output.getPaths().size());
        assertEquals(0, output.getPaths().get(0).getId().getValue());
    }
    @Test
    public void shouldExposeMetadataAndRejectSvgDocuments() {
        StageDescriptor descriptor=new StageDescriptor("contour", "Contour", "Thresholds pixels", true);
        ContourStage stage=new ContourStage(descriptor, 128);
        VectorDocument svg=new VectorDocument("svg", new SvgOrigin("svg"), java.util.Collections.<com.vectorium.core.model.VectorPath>emptyList(), 0, 0, new int[0]);
        assertEquals("contour", stage.getName());
        assertEquals("Contour", stage.getLabel());
        assertEquals("Thresholds pixels", stage.getDescription());
        assertEquals(StageTag.RASTER_ONLY, stage.getTag());
        assertFalse(stage.appliesTo(svg));
        try {
            stage.apply(svg);
            fail("Expected raster stage to reject SVG");
        } catch (StageException exception) {
            assertTrue(exception.getMessage().length()>0);
        }
    }
    private VectorDocument raster(int[] pixels) {
        int width=Math.max(1, pixels.length/2);
        int height=pixels.length/width;
        return new VectorDocument("raster", new RasterOrigin("raster"), java.util.Collections.<com.vectorium.core.model.VectorPath>emptyList(), width, height, pixels);
    }
}
