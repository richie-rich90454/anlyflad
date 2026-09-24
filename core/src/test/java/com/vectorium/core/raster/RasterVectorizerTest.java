package com.vectorium.core.raster;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import com.vectorium.core.model.Color;
import com.vectorium.core.model.Rect;
import com.vectorium.core.model.VectorPath;
public final class RasterVectorizerTest {
    @Test
    public void shouldCreateOneClosedBlackRectangleForSolidForeground() {
        int[] pixels={black(), black(), black(), black(), black(), black()};
        List<VectorPath> paths=RasterVectorizer.vectorize(RasterFrame.wrap(3, 2, pixels));
        assertEquals(1, paths.size());
        assertRectangle(paths.get(0), 0, 0.0, 0.0, 3.0, 2.0);
    }
    @Test
    public void shouldSplitRunsAndMergeMatchingVerticalRuns() {
        int[] pixels={black(), black(), black(), black(), black(), black(), black(), white(), black(), black(), black(), black(), white(), black(), black()};
        List<VectorPath> paths=RasterVectorizer.vectorize(RasterFrame.wrap(5, 3, pixels));
        assertEquals(3, paths.size());
        assertRectangle(paths.get(0), 0, 0.0, 0.0, 5.0, 1.0);
        assertRectangle(paths.get(1), 1, 0.0, 1.0, 2.0, 3.0);
        assertRectangle(paths.get(2), 2, 3.0, 1.0, 5.0, 3.0);
    }
    @Test
    public void shouldLeaveHolesAndBackgroundUnvectorized() {
        int[] pixels=new int[25];
        for (int y=0;y<5;y++) {
            for (int x=0;x<5;x++) {
                boolean border=x==0||x==4||y==0||y==4;
                pixels[y*5+x]=border?black():white();
            }
        }
        List<VectorPath> paths=RasterVectorizer.vectorize(RasterFrame.wrap(5, 5, pixels));
        assertEquals(4, paths.size());
        assertRectangle(paths.get(0), 0, 0.0, 0.0, 5.0, 1.0);
        assertRectangle(paths.get(1), 1, 0.0, 1.0, 1.0, 4.0);
        assertRectangle(paths.get(2), 2, 4.0, 1.0, 5.0, 4.0);
        assertRectangle(paths.get(3), 3, 0.0, 4.0, 5.0, 5.0);
    }
    @Test
    public void shouldReturnDeterministicSequentialPathIds() {
        int[] pixels={black(), black(), white(), black(), black(), black(), black(), white(), black()};
        List<VectorPath> first=RasterVectorizer.vectorize(RasterFrame.wrap(3, 3, pixels));
        List<VectorPath> second=RasterVectorizer.vectorize(RasterFrame.wrap(3, 3, pixels));
        assertEquals(first.size(), second.size());
        for (int index=0;index<first.size();index++) {
            assertEquals(index, first.get(index).getId().getValue());
            assertEquals(first.get(index).getId(), second.get(index).getId());
            assertArrayEquals(first.get(index).getCoordinates(), second.get(index).getCoordinates());
        }
    }
    @Test
    public void shouldRejectNullAndInvalidDimensions() {
        try {
            RasterVectorizer.vectorize((RasterFrame) null);
            fail("Expected null frame to be rejected");
        } catch (IllegalArgumentException exception) {
            assertTrue(exception.getMessage().length()>0);
        }
        try {
            RasterVectorizer.vectorize(0, 1, new int[0]);
            fail("Expected zero width to be rejected");
        } catch (IllegalArgumentException exception) {
            assertTrue(exception.getMessage().length()>0);
        }
        try {
            RasterVectorizer.vectorize(1, -1, new int[0]);
            fail("Expected negative height to be rejected");
        } catch (IllegalArgumentException exception) {
            assertTrue(exception.getMessage().length()>0);
        }
        try {
            RasterVectorizer.vectorize(2, 2, new int[3]);
            fail("Expected mismatched buffer to be rejected");
        } catch (IllegalArgumentException exception) {
            assertTrue(exception.getMessage().length()>0);
        }
        try {
            RasterVectorizer.vectorize(1, 1, null);
            fail("Expected null buffer to be rejected");
        } catch (IllegalArgumentException exception) {
            assertTrue(exception.getMessage().length()>0);
        }
        try {
            RasterVectorizer.vectorize(Integer.MAX_VALUE, 2, new int[0]);
            fail("Expected overflowing dimensions to be rejected");
        } catch (IllegalArgumentException exception) {
            assertTrue(exception.getMessage().length()>0);
        }
    }
    private static int black() {
        return 0xFF000000;
    }
    private static int white() {
        return 0xFFFFFFFF;
    }
    private static void assertRectangle(VectorPath path, int id, double minX, double minY, double maxX, double maxY) {
        assertEquals(id, path.getId().getValue());
        assertTrue(path.isClosed());
        assertEquals(new Color(0, 0, 0, 255), path.getFill());
        assertEquals(1.0, path.getOpacity(), 0.0);
        assertEquals(new Rect(minX, minY, maxX, maxY), path.getBounds());
        assertArrayEquals(new double[] {minX, minY, maxX, minY, maxX, maxY, minX, maxY}, path.getCoordinates());
    }
}
