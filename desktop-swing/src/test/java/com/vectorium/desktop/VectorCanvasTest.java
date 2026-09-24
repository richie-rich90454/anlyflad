package com.vectorium.desktop;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.Collections;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import com.vectorium.core.model.PathId;
import com.vectorium.core.model.SvgOrigin;
import com.vectorium.core.model.VectorDocument;
import com.vectorium.core.model.VectorPath;
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
        VectorPath path=new VectorPath(PathId.of(0), new double[]{10.0, 10.0, 90.0, 10.0, 90.0, 90.0, 10.0, 90.0}, true, new com.vectorium.core.model.Color(214, 90, 49, 255), 1.0);
        VectorDocument vector=new VectorDocument("vector.svg", new SvgOrigin("vector.svg"), Collections.singletonList(path), 100, 100, new int[10000]);
        vectorCanvas.setDocument(vector);
        vectorCanvas.fitToViewport();
        BufferedImage vectorImage=paint(vectorCanvas, 320, 240);
        assertTrue(countPixels(vectorImage, 0xFFD65A31)>0);
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
