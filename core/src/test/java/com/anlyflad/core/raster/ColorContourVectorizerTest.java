package com.anlyflad.core.raster;

import com.anlyflad.core.model.VectorPath;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class ColorContourVectorizerTest {
    private static final int RED = 0xFFFF0000;
    private static final int GREEN = 0xFF00FF00;
    private static final int BLUE = 0xFF0000FF;

    @Test
    public void shouldVectorizeSolidRectangleWithoutPerPixelPaths() {
        int width = 64;
        int height = 48;
        int[] pixels = new int[width * height];
        for (int index = 0; index < pixels.length; index++) {
            pixels[index] = RED;
        }

        List<VectorPath> paths = ColorContourVectorizer.vectorize(RasterFrame.wrap(width, height, pixels));

        assertEquals(1, paths.size());
        VectorPath path = paths.get(0);
        assertTrue(path.isCompound());
        assertTrue(path.isClosed());
        assertEquals(VectorPath.FillRule.EVEN_ODD, path.getFillRule());
        assertEquals(RED, path.getFill().toArgb());
        assertEquals(1, path.getRingCount());
        assertEquals(4, path.getNodeCount());
        assertRings(new double[][] {{0.0, 0.0, width, 0.0, width, height, 0.0, height}}, path);
    }

    @Test
    public void shouldGroupDisconnectedSameColorIslandsIntoOnePath() {
        int[] pixels = {RED, BLUE, RED};

        List<VectorPath> paths = ColorContourVectorizer.vectorize(RasterFrame.wrap(3, 1, pixels));

        assertEquals(2, paths.size());
        assertEquals(RED, paths.get(0).getFill().toArgb());
        assertEquals(2, paths.get(0).getRingCount());
        assertEquals(8, paths.get(0).getNodeCount());
        assertRings(new double[][] {
            {0.0, 0.0, 1.0, 0.0, 1.0, 1.0, 0.0, 1.0},
            {2.0, 0.0, 3.0, 0.0, 3.0, 1.0, 2.0, 1.0}
        }, paths.get(0));
        assertEquals(BLUE, paths.get(1).getFill().toArgb());
    }

    @Test
    public void shouldPreserveHoleAsAnEvenOddInnerRing() {
        int[] pixels = new int[25];
        for (int y = 0; y < 5; y++) {
            for (int x = 0; x < 5; x++) {
                if (x == 0 || x == 4 || y == 0 || y == 4) {
                    pixels[y * 5 + x] = RED;
                }
            }
        }

        VectorPath path = ColorContourVectorizer.vectorize(RasterFrame.wrap(5, 5, pixels)).get(0);

        assertEquals(2, path.getRingCount());
        assertEquals(8, path.getNodeCount());
        assertEquals(16.0, path.getArea(), 0.0);
        assertRings(new double[][] {
            {0.0, 0.0, 5.0, 0.0, 5.0, 5.0, 0.0, 5.0},
            {1.0, 4.0, 4.0, 4.0, 4.0, 1.0, 1.0, 1.0}
        }, path);
    }

    @Test
    public void shouldIgnoreFullyTransparentPixelsWhenConnectingRegions() {
        int[] pixels = {RED, 0x00FFFFFF, RED, 0x00000000, RED};

        List<VectorPath> paths = ColorContourVectorizer.vectorize(RasterFrame.wrap(5, 1, pixels));
        VectorPath path = paths.get(0);

        assertEquals(1, paths.size());
        assertEquals(3, path.getRingCount());
        assertEquals(12, path.getNodeCount());
        assertRings(new double[][] {
            {0.0, 0.0, 1.0, 0.0, 1.0, 1.0, 0.0, 1.0},
            {2.0, 0.0, 3.0, 0.0, 3.0, 1.0, 2.0, 1.0},
            {4.0, 0.0, 5.0, 0.0, 5.0, 1.0, 4.0, 1.0}
        }, path);
        assertTrue(ColorContourVectorizer.vectorize(RasterFrame.wrap(1, 1, new int[] {0x00FFFFFF})).isEmpty());
    }

    @Test
    public void shouldKeepDiagonalSameColorPixelsAsSeparateFourConnectedComponents() {
        int[] pixels = {RED, 0, 0, RED};

        VectorPath path = ColorContourVectorizer.vectorize(RasterFrame.wrap(2, 2, pixels)).get(0);

        assertEquals(2, path.getRingCount());
        assertEquals(8, path.getNodeCount());
        assertRings(new double[][] {
            {0.0, 0.0, 1.0, 0.0, 1.0, 1.0, 0.0, 1.0},
            {1.0, 1.0, 2.0, 1.0, 2.0, 2.0, 1.0, 2.0}
        }, path);
    }

    @Test
    public void shouldPreserveFullArgbEqualityAndDeterministicFirstSeenOrder() {
        int translucentRed = 0x80FF0000;
        int[] firstPixels = {
            GREEN, RED, 0, BLUE, RED, GREEN,
            GREEN, 0, 0, 0, RED, GREEN
        };

        List<VectorPath> first = ColorContourVectorizer.vectorize(RasterFrame.wrap(6, 2, firstPixels));
        List<VectorPath> second = ColorContourVectorizer.vectorize(RasterFrame.wrap(6, 2, firstPixels));
        List<VectorPath> alphaPixels = ColorContourVectorizer.vectorize(RasterFrame.wrap(2, 1, new int[] {RED, translucentRed}));

        assertEquals(3, first.size());
        assertEquals(GREEN, first.get(0).getFill().toArgb());
        assertEquals(RED, first.get(1).getFill().toArgb());
        assertEquals(BLUE, first.get(2).getFill().toArgb());
        assertEquals(2, first.get(0).getRingCount());
        assertEquals(2, first.get(1).getRingCount());
        for (int index = 0; index < first.size(); index++) {
            assertEquals(index, first.get(index).getId().getValue());
            assertEquals(first.get(index), second.get(index));
            assertRings(first.get(index).getRingCoordinates(), second.get(index));
        }
        assertEquals(2, alphaPixels.size());
        assertEquals(RED, alphaPixels.get(0).getFill().toArgb());
        assertEquals(translucentRed, alphaPixels.get(1).getFill().toArgb());
    }

    @Test
    public void shouldEnumerateComponentsAcrossVisitedBitsetWords() {
        int width = 70;
        int[] pixels = new int[width];
        for (int x = 0; x < width; x += 2) {
            pixels[x] = RED;
        }

        VectorPath path = ColorContourVectorizer.vectorize(RasterFrame.wrap(width, 1, pixels), 1, 140).get(0);

        assertEquals(35, path.getRingCount());
        assertEquals(140, path.getNodeCount());
        assertArrayEquals(
            new double[] {68.0, 0.0, 69.0, 0.0, 69.0, 1.0, 68.0, 1.0},
            path.getRingCoordinates()[path.getRingCount() - 1],
            0.0
        );
    }

    @Test
    public void shouldEnforcePathAndVertexBudgetsWithResourceErrors() {
        int[] colors = {RED, BLUE};
        ColorContourVectorizer.ResourceLimitException pathError = assertThrows(
            ColorContourVectorizer.ResourceLimitException.class,
            () -> ColorContourVectorizer.vectorize(RasterFrame.wrap(2, 1, colors), 1, 20)
        );
        assertEquals("paths", pathError.getResource());
        assertEquals(1, pathError.getLimit());
        assertTrue(pathError.getMessage().contains("path budget"));

        int[] ell = {RED, RED, RED, 0};
        RasterFrame ellFrame = RasterFrame.wrap(2, 2, ell);
        ColorContourVectorizer.ResourceLimitException vertexError = assertThrows(
            ColorContourVectorizer.ResourceLimitException.class,
            () -> ColorContourVectorizer.vectorize(ellFrame, 1, 5)
        );
        assertEquals("vertices", vertexError.getResource());
        assertEquals(5, vertexError.getLimit());
        assertTrue(vertexError.getMessage().contains("vertex budget"));
        assertEquals(6, ColorContourVectorizer.vectorize(ellFrame, 1, 6).get(0).getNodeCount());
    }

    @Test
    public void shouldRejectInvalidFrameDimensionsArgbAndBudgets() {
        assertThrows(IllegalArgumentException.class, () -> ColorContourVectorizer.vectorize(null, 1, 10));
        assertThrows(IllegalArgumentException.class, () -> ColorContourVectorizer.vectorize(0, 1, new int[0], 1, 10));
        assertThrows(IllegalArgumentException.class, () -> ColorContourVectorizer.vectorize(1, -1, new int[0], 1, 10));
        assertThrows(IllegalArgumentException.class, () -> ColorContourVectorizer.vectorize(1, 1, null, 1, 10));
        assertThrows(IllegalArgumentException.class, () -> ColorContourVectorizer.vectorize(2, 2, new int[3], 1, 10));
        assertThrows(IllegalArgumentException.class, () -> ColorContourVectorizer.vectorize(Integer.MAX_VALUE, 2, new int[0], 1, 10));
        assertThrows(IllegalArgumentException.class, () -> ColorContourVectorizer.vectorize(RasterFrame.wrap(1, 1, new int[] {RED}), 1, 0));
        assertThrows(IllegalArgumentException.class, () -> ColorContourVectorizer.vectorize(RasterFrame.wrap(1, 1, new int[] {RED}), ColorContourVectorizer.MAX_PATHS + 1, 10));
        assertThrows(IllegalArgumentException.class, () -> ColorContourVectorizer.vectorize(RasterFrame.wrap(1, 1, new int[] {RED}), 1, ColorContourVectorizer.MAX_VERTICES + 1));
        IllegalArgumentException invalidBudget = assertThrows(
            IllegalArgumentException.class,
            () -> ColorContourVectorizer.vectorize(RasterFrame.wrap(1, 1, new int[] {RED}), 0, 10)
        );
        assertFalse(invalidBudget instanceof ColorContourVectorizer.ResourceLimitException);
    }

    private static void assertRings(double[][] expected, VectorPath path) {
        double[][] actual = path.getRingCoordinates();
        assertEquals(expected.length, actual.length);
        for (int index = 0; index < expected.length; index++) {
            assertArrayEquals(expected[index], actual[index], 0.0);
        }
    }
}
