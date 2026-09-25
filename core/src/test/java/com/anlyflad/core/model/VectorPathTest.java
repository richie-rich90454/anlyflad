package com.anlyflad.core.model;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
public final class VectorPathTest {
    @Test
    public void shouldStoreGeometryStyleBoundsAreaAndNodeCount() {
        VectorPath path=new VectorPath(PathId.of(1), new double[]{0.0, 0.0, 4.0, 0.0, 4.0, 3.0, 0.0, 3.0}, true, new Color(1, 2, 3, 4), 0.75);
        assertEquals(PathId.of(1), path.getId());
        assertTrue(path.isClosed());
        assertEquals(new Color(1, 2, 3, 4), path.getFill());
        assertEquals(0.75, path.getOpacity());
        assertEquals(new Rect(0.0, 0.0, 4.0, 3.0), path.getBounds());
        assertEquals(12.0, path.getArea());
        assertEquals(4, path.getNodeCount());
    }
    @Test
    public void shouldUseZeroAreaForOpenOrDegenerateGeometry() {
        VectorPath openPath=new VectorPath(PathId.zero(), new double[]{0.0, 0.0, 2.0, 0.0, 2.0, 2.0}, false, new Color(0, 0, 0, 0), 1.0);
        VectorPath pointPath=new VectorPath(PathId.of(1), new double[]{4.0, 5.0}, true, new Color(0, 0, 0, 255), 1.0);
        assertEquals(0.0, openPath.getArea());
        assertEquals(0.0, pointPath.getArea());
        assertEquals(new Rect(4.0, 5.0, 4.0, 5.0), pointPath.getBounds());
    }
    @Test
    public void shouldDefensivelyCopyCoordinatesOnInputAndOutput() {
        double[] coordinates={0.0, 0.0, 2.0, 0.0, 2.0, 2.0};
        VectorPath path=new VectorPath(PathId.zero(), coordinates, true, new Color(0, 0, 0, 255), 1.0);
        coordinates[0]=100.0;
        double[] exposedCoordinates=path.getCoordinates();
        exposedCoordinates[1]=100.0;
        assertArrayEquals(new double[]{0.0, 0.0, 2.0, 0.0, 2.0, 2.0}, path.getCoordinates());
        assertNotSame(coordinates, path.getCoordinates());
    }
    @Test
    public void shouldCreateChangedGeometryAndStyleWithoutMutation() {
        VectorPath original=new VectorPath(PathId.of(3), new double[]{0.0, 0.0, 2.0, 0.0, 2.0, 2.0}, true, new Color(1, 1, 1, 255), 1.0);
        VectorPath changedGeometry=original.withGeometry(new double[]{0.0, 0.0, 4.0, 0.0, 4.0, 4.0, 0.0, 4.0}, false);
        VectorPath changedStyle=original.withStyle(new Color(9, 8, 7, 6), 0.25);
        assertEquals(4, changedGeometry.getNodeCount());
        assertFalse(changedGeometry.isClosed());
        assertEquals(16.0, changedGeometry.getBounds().getArea());
        assertEquals(0.0, changedGeometry.getArea());
        assertEquals(new Color(9, 8, 7, 6), changedStyle.getFill());
        assertEquals(0.25, changedStyle.getOpacity());
        assertEquals(3, original.getNodeCount());
        assertTrue(original.isClosed());
        assertEquals(new Color(1, 1, 1, 255), original.getFill());
        assertEquals(1.0, original.getOpacity());
    }
    @Test
    public void shouldRejectInvalidGeometryAndStyle() {
        try {
            new VectorPath(null, new double[]{0.0, 0.0}, false, new Color(0, 0, 0, 255), 1.0);
            fail("Expected null id to be rejected");
        } catch (IllegalArgumentException exception) {
            assertEquals(true, exception.getMessage().length()>0);
        }
        try {
            new VectorPath(PathId.zero(), null, false, new Color(0, 0, 0, 255), 1.0);
            fail("Expected null coordinates to be rejected");
        } catch (IllegalArgumentException exception) {
            assertEquals(true, exception.getMessage().length()>0);
        }
        try {
            new VectorPath(PathId.zero(), new double[0], false, new Color(0, 0, 0, 255), 1.0);
            fail("Expected empty coordinates to be rejected");
        } catch (IllegalArgumentException exception) {
            assertEquals(true, exception.getMessage().length()>0);
        }
        try {
            new VectorPath(PathId.zero(), new double[]{0.0}, false, new Color(0, 0, 0, 255), 1.0);
            fail("Expected odd coordinate count to be rejected");
        } catch (IllegalArgumentException exception) {
            assertEquals(true, exception.getMessage().length()>0);
        }
        double[] nonFiniteCoordinates={Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY};
        for (double value : nonFiniteCoordinates) {
            try {
                new VectorPath(PathId.zero(), new double[]{value, 0.0}, false, new Color(0, 0, 0, 255), 1.0);
                fail("Expected non-finite coordinate to be rejected");
            } catch (IllegalArgumentException exception) {
                assertEquals(true, exception.getMessage().length()>0);
            }
        }
        try {
            new VectorPath(PathId.zero(), new double[]{0.0, 0.0}, false, null, 1.0);
            fail("Expected null fill to be rejected");
        } catch (IllegalArgumentException exception) {
            assertEquals(true, exception.getMessage().length()>0);
        }
        double[] invalidOpacities={Double.NaN, Double.POSITIVE_INFINITY, -0.01, 1.01};
        for (double opacity : invalidOpacities) {
            try {
                new VectorPath(PathId.zero(), new double[]{0.0, 0.0}, false, new Color(0, 0, 0, 255), opacity);
                fail("Expected invalid opacity to be rejected");
            } catch (IllegalArgumentException exception) {
                assertEquals(true, exception.getMessage().length()>0);
            }
        }
    }
    @Test
    public void shouldStoreCompoundRingsWithHoleAndDefensiveCopies() {
        double[][] rings={
            {0.0, 0.0, 10.0, 0.0, 10.0, 10.0, 0.0, 10.0},
            {2.0, 2.0, 8.0, 2.0, 8.0, 8.0, 2.0, 8.0}
        };
        VectorPath path=new VectorPath(PathId.of(4), rings, new Color(20, 40, 60, 255), 1.0, VectorPath.FillRule.EVEN_ODD);
        assertTrue(path.isCompound());
        assertTrue(path.isClosed());
        assertEquals(2, path.getRingCount());
        assertEquals(8, path.getNodeCount());
        assertEquals(new Rect(0.0, 0.0, 10.0, 10.0), path.getBounds());
        assertEquals(64.0, path.getArea(), 0.0);
        assertEquals(VectorPath.FillRule.EVEN_ODD, path.getFillRule());
        List<double[]> exposed=path.getRings();
        assertNotSame(rings[0], exposed.get(0));
        rings[0][0]=100.0;
        exposed.get(1)[0]=100.0;
        assertEquals(0.0, path.getRing(0)[0], 0.0);
        assertEquals(2.0, path.getRing(1)[0], 0.0);
        try {
            exposed.clear();
            fail("Expected compound ring list to be unmodifiable");
        } catch (UnsupportedOperationException exception) {
            assertTrue(exception.getClass()==UnsupportedOperationException.class);
        }
        VectorPath same=new VectorPath(PathId.of(4), new double[][]{{0.0, 0.0, 10.0, 0.0, 10.0, 10.0, 0.0, 10.0}, {2.0, 2.0, 8.0, 2.0, 8.0, 8.0, 2.0, 8.0}}, new Color(20, 40, 60, 255), 1.0, VectorPath.FillRule.EVEN_ODD);
        assertEquals(path, same);
        assertEquals(path.hashCode(), same.hashCode());
        assertNotEquals(path, same.withFillRule(VectorPath.FillRule.NONZERO));
        assertEquals(100.0, new VectorPath(PathId.of(5), new double[][]{{0.0, 0.0, 10.0, 0.0, 10.0, 10.0, 0.0, 10.0}, {2.0, 2.0, 8.0, 2.0, 8.0, 8.0, 2.0, 8.0}}, new Color(20, 40, 60, 255), 1.0, VectorPath.FillRule.NONZERO).getArea(), 0.0);
        assertEquals(64.0, new VectorPath(PathId.of(6), new double[][]{{0.0, 0.0, 10.0, 0.0, 10.0, 10.0, 0.0, 10.0}, {2.0, 2.0, 2.0, 8.0, 8.0, 8.0, 8.0, 2.0}}, new Color(20, 40, 60, 255), 1.0, VectorPath.FillRule.NONZERO).getArea(), 0.0);
    }
    @Test
    public void shouldDefensivelyCopyCubicSegmentsAndIncludeThemInEquality() {
        double[] fallback={0.0,0.0,10.0,0.0,10.0,10.0,0.0,10.0};
        double[] cubic={0.0,0.0,0.0,0.0,10.0/3.0,0.0,10.0,0.0,10.0,0.0,20.0,0.0,20.0,10.0/3.0,10.0,10.0,10.0,10.0,10.0,20.0,10.0,20.0/3.0,0.0,10.0,0.0,10.0,0.0,20.0,0.0,10.0/3.0,0.0,0.0};
        VectorPath path=new VectorPath(PathId.of(7),Arrays.asList(fallback),new double[][]{cubic},new Color(1,2,3,255),1.0,VectorPath.FillRule.EVEN_ODD);
        path.getCubicRings().get(0)[0]=99.0;
        path.getCubicSegmentLists().get(0).get(0)[0]=98.0;
        assertEquals(0.0,path.getCubicRing(0)[0],0.0);
        assertEquals(0.0,path.getCubicRingCoordinates()[0][0],0.0);
        assertTrue(path.hasCubicData());
        VectorPath same=new VectorPath(PathId.of(7),Arrays.asList(fallback),new double[][]{cubic},new Color(1,2,3,255),1.0,VectorPath.FillRule.EVEN_ODD);
        assertEquals(path,same);
        assertEquals(path.hashCode(),same.hashCode());
        assertNotEquals(path,path.withoutCubicData());
    }

    @Test
    public void shouldUseValueEqualityHashCodeAndString() {
        VectorPath first=new VectorPath(PathId.of(2), new double[]{0.0, 0.0, 2.0, 0.0, 2.0, 2.0, 0.0, 2.0}, true, new Color(1, 2, 3, 4), 0.5);
        VectorPath second=new VectorPath(PathId.of(2), new double[]{0.0, 0.0, 2.0, 0.0, 2.0, 2.0, 0.0, 2.0}, true, new Color(1, 2, 3, 4), 0.5);
        assertEquals(first, second);
        assertEquals(first.hashCode(), second.hashCode());
        assertNotEquals(first, second.withStyle(new Color(9, 8, 7, 6), 0.5));
        assertEquals("VectorPath{id=PathId{value=2}, coordinates=[0.0, 0.0, 2.0, 0.0, 2.0, 2.0, 0.0, 2.0], closed=true, fill=Color{red=1, green=2, blue=3, alpha=4}, opacity=0.5, bounds=Rect{minX=0.0, minY=0.0, maxX=2.0, maxY=2.0}, area=4.0, nodeCount=4}", first.toString());
    }
}
