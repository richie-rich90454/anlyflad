package com.anlyflad.desktop;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.Collections;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import com.anlyflad.core.model.PathId;
import com.anlyflad.core.model.RasterOrigin;
import com.anlyflad.core.model.SvgOrigin;
import com.anlyflad.core.model.VectorDocument;
import com.anlyflad.core.model.VectorPath;
public final class VectorCanvasTest {
    @Test
    public void shouldPaintEmptyStateAndExposeAccessibleMetadata() {
        VectorCanvas canvas=new VectorCanvas();
        canvas.setSize(320, 240);
        canvas.setDocument(null);
        BufferedImage image=paint(canvas, 320, 240);
        assertTrue(countPixels(image, canvas.getBackground().getRGB())<image.getWidth()*image.getHeight());
        assertEquals("Vector preview canvas", canvas.getAccessibleContext().getAccessibleName());
        assertTrue(canvas.getToolTipText().contains("zoom"));
    }
    @Test
    public void shouldRenderRasterPixelsAndVectorPaths() {
        VectorCanvas rasterCanvas=new VectorCanvas();
        rasterCanvas.setSize(320, 240);
        VectorDocument raster=VectorDocument.emptyRaster("raster.png", 2, 2);
        raster=raster.withPixels(new int[]{0xFF102030, 0xFFFFFFFF, 0xFF506070, 0xFFA0B0C0});
        rasterCanvas.setDocument(raster);
        rasterCanvas.fitToViewport();
        BufferedImage rasterImage=paint(rasterCanvas, 320, 240);
        assertTrue(countPixels(rasterImage, 0xFF102030)>0);
        VectorCanvas vectorCanvas=new VectorCanvas();
        vectorCanvas.setSize(320, 240);
        VectorPath path=new VectorPath(PathId.of(0), new double[]{10.0, 10.0, 90.0, 10.0, 90.0, 90.0, 10.0, 90.0}, true, new com.anlyflad.core.model.Color(214, 90, 49, 255), 1.0);
        VectorDocument vector=new VectorDocument("vector.svg", new SvgOrigin("vector.svg"), Collections.singletonList(path), 100, 100, new int[10000]);
        vectorCanvas.setDocument(vector);
        vectorCanvas.fitToViewport();
        BufferedImage vectorImage=paint(vectorCanvas, 320, 240);
        assertTrue(countPixels(vectorImage, 0xFFD65A31)>0);
    }
    @Test
    public void shouldRenderRasterOriginVectorResultsWithoutStalePixels() {
        VectorCanvas canvas=new VectorCanvas();
        canvas.setSize(320, 240);
        VectorPath path=new VectorPath(PathId.of(0), new double[]{0.0, 0.0, 2.0, 0.0, 2.0, 2.0, 0.0, 2.0}, true, new com.anlyflad.core.model.Color(12, 34, 56, 255), 1.0);
        VectorDocument rasterResult=new VectorDocument("raster.png", new RasterOrigin("raster.png"), Collections.singletonList(path), 2, 2, new int[]{0xFFFFFFFF, 0xFFFFFFFF, 0xFFFFFFFF, 0xFFFFFFFF});
        canvas.setVectorResult(rasterResult);
        canvas.fitToViewport();
        BufferedImage image=paint(canvas, 320, 240);
        assertTrue(countPixels(image, 0xFF0C2238)>0);
    }
    @Test
    public void shouldRenderCompoundPathsUsingTheExplicitWindingRule() {
        int fill=0xFF3366CC;
        VectorPath evenOdd=compoundPath(fill, com.anlyflad.core.model.VectorPath.FillRule.EVEN_ODD);
        BufferedImage evenOddImage=render(evenOdd);
        assertEquals(fill, evenOddImage.getRGB(32, 32));
        assertNotEquals(fill, evenOddImage.getRGB(60, 60));
        VectorPath nonzero=compoundPath(fill, com.anlyflad.core.model.VectorPath.FillRule.NONZERO);
        BufferedImage nonzeroImage=render(nonzero);
        assertEquals(fill, nonzeroImage.getRGB(60, 60));
    }
    @Test
    public void shouldRenderCubicSubpathsWithJava2D() {
        int fill=0xFF3366CC;
        double[] fallback={1.0,5.0,5.0,1.0,9.0,5.0,5.0,9.0};
        double[] cubic={
            1.0,5.0,1.0,2.238095,2.238095,1.0,5.0,1.0,
            5.0,1.0,7.761905,1.0,9.0,2.238095,9.0,5.0,
            9.0,5.0,9.0,7.761905,7.761905,9.0,5.0,9.0,
            5.0,9.0,2.238095,9.0,1.0,7.761905,1.0,5.0
        };
        VectorPath path=new VectorPath(PathId.of(0),Arrays.asList(fallback),new double[][]{cubic},new com.anlyflad.core.model.Color(51,102,204,255),1.0,VectorPath.FillRule.EVEN_ODD);
        BufferedImage image=render(path);
        assertEquals(fill,image.getRGB(60,60));
        assertNotEquals(fill,image.getRGB(32,32));
    }

    @Test
    public void shouldFitAndClampZoom() {
        VectorCanvas canvas=new VectorCanvas();
        canvas.setSize(320, 240);
        canvas.setDocument(VectorDocument.emptySvg("drawing.svg", 100, 100));
        canvas.fitToViewport();
        assertTrue(canvas.getZoom()>1.0);
        assertTrue(canvas.getPanX()>0.0);
        canvas.setZoom(0.01);
        assertEquals(0.1, canvas.getZoom(), 0.0);
        canvas.setZoom(100.0);
        assertEquals(16.0, canvas.getZoom(), 0.0);
        try {
            canvas.setZoom(Double.NaN);
            fail("Expected invalid zoom to be rejected");
        } catch (IllegalArgumentException exception) {
            assertNotNull(exception.getMessage());
        }
    }
    private static VectorPath compoundPath(int fill, com.anlyflad.core.model.VectorPath.FillRule fillRule) {
        return new VectorPath(PathId.of(0), Arrays.asList(
            new double[]{0.0, 0.0, 10.0, 0.0, 10.0, 10.0, 0.0, 10.0},
            new double[]{2.0, 2.0, 8.0, 2.0, 8.0, 8.0, 2.0, 8.0}), new com.anlyflad.core.model.Color((fill>>>16)&0xFF, (fill>>>8)&0xFF, fill&0xFF, 255), 1.0, fillRule);
    }
    private static BufferedImage render(VectorPath path) {
        VectorCanvas canvas=new VectorCanvas();
        canvas.setSize(120, 120);
        VectorDocument document=new VectorDocument("compound.svg", new SvgOrigin("compound.svg"), Collections.singletonList(path), 10, 10, new int[100]);
        canvas.setDocument(document);
        return paint(canvas, 120, 120);
    }
    private static BufferedImage paint(VectorCanvas canvas, int width, int height) {
        BufferedImage image=new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics=image.createGraphics();
        try {
            canvas.paint(graphics);
        } finally {
            graphics.dispose();
        }
        return image;
    }
    private static int countPixels(BufferedImage image, int rgb) {
        int count=0;
        for (int y=0;y<image.getHeight();y++) {
            for (int x=0;x<image.getWidth();x++) {
                if (image.getRGB(x, y)==rgb) {
                    count++;
                }
            }
        }
        return count;
    }
}
