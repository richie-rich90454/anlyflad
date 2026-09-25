package com.vectorium.core.raster;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import com.vectorium.core.model.VectorPath;
public final class ColorRasterVectorizerTest {
    @Test
    public void shouldPreserveExactColorsAndMergeMatchingRuns() {
        int red=0xFFFF0000;
        int green=0xFF00FF00;
        int[] pixels={red, red, green, red, red, green};
        List<VectorPath> paths=ColorRasterVectorizer.vectorize(RasterFrame.wrap(3, 2, pixels));
        assertEquals(2, paths.size());
        assertColor(paths.get(0), red, 0.0, 0.0, 2.0, 2.0);
        assertColor(paths.get(1), green, 2.0, 0.0, 3.0, 2.0);
    }
    @Test
    public void shouldSkipTransparentPixelsAndCoverVisiblePixelsExactly() {
        int visible=0xFF123456;
        int[] pixels={0x00000000, 0x00FFFFFF, visible, visible, 0x00000000, visible, 0x80123456, 0x00000000};
        List<VectorPath> paths=ColorRasterVectorizer.vectorize(RasterFrame.wrap(4, 2, pixels), 10);
        assertEquals(3, paths.size());
        int[] reconstructed=new int[8];
        boolean[] covered=new boolean[8];
        for (int index=0;index<paths.size();index++) {
            VectorPath path=paths.get(index);
            int left=(int)path.getBounds().getMinX();
            int top=(int)path.getBounds().getMinY();
            int right=(int)path.getBounds().getMaxX();
            int bottom=(int)path.getBounds().getMaxY();
            for (int y=top;y<bottom;y++) {
                for (int x=left;x<right;x++) {
                    int offset=y*4+x;
                    assertTrue(!covered[offset]);
                    covered[offset]=true;
                    reconstructed[offset]=path.getFill().toArgb();
                }
            }
        }
        assertEquals(visible, reconstructed[2]);
        assertEquals(visible, reconstructed[3]);
        assertEquals(visible, reconstructed[5]);
        assertEquals(0x80123456, reconstructed[6]);
        assertTrue(!covered[0]);
        assertTrue(!covered[1]);
        assertTrue(!covered[4]);
        assertTrue(!covered[7]);
    }
    @Test
    public void shouldEnforcePathLimitAndValidateArguments() {
        int[] pixels={0xFFFF0000, 0xFF00FF00};
        try {
            ColorRasterVectorizer.vectorize(RasterFrame.wrap(2, 1, pixels), 1);
            fail("Expected path limit to be enforced");
        } catch (IllegalArgumentException exception) {
            assertTrue(exception.getMessage().contains("path limit"));
        }
        try {
            ColorRasterVectorizer.vectorize(RasterFrame.wrap(2, 1, pixels), 0);
            fail("Expected invalid path limit to be rejected");
        } catch (IllegalArgumentException exception) {
            assertTrue(exception.getMessage().length()>0);
        }
        try {
            ColorRasterVectorizer.vectorize(RasterFrame.wrap(2, 1, new int[1]), 10);
            fail("Expected mismatched pixels to be rejected");
        } catch (IllegalArgumentException exception) {
            assertTrue(exception.getMessage().length()>0);
        }
    }
    private static void assertColor(VectorPath path, int argb, double minX, double minY, double maxX, double maxY) {
        assertEquals(argb, path.getFill().toArgb());
        assertEquals(1.0, path.getOpacity(), 0.0);
        assertEquals(minX, path.getBounds().getMinX(), 0.0);
        assertEquals(minY, path.getBounds().getMinY(), 0.0);
        assertEquals(maxX, path.getBounds().getMaxX(), 0.0);
        assertEquals(maxY, path.getBounds().getMaxY(), 0.0);
    }
}
