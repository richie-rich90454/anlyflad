package com.anlyflad.core.model;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
public final class RectTest {
    @Test
    public void shouldCalculateDimensionsAndArea() {
        Rect rectangle=new Rect(1.0, 2.0, 5.0, 7.0);
        assertEquals(1.0, rectangle.getMinX());
        assertEquals(2.0, rectangle.getMinY());
        assertEquals(5.0, rectangle.getMaxX());
        assertEquals(7.0, rectangle.getMaxY());
        assertEquals(4.0, rectangle.getWidth());
        assertEquals(5.0, rectangle.getHeight());
        assertEquals(20.0, rectangle.getArea());
    }
    @Test
    public void shouldTestPointAndRectangleContainment() {
        Rect rectangle=new Rect(0.0, 0.0, 2.0, 2.0);
        assertTrue(rectangle.contains(new Point(1.0, 1.0)));
        assertTrue(rectangle.contains(new Point(0.0, 2.0)));
        assertFalse(rectangle.contains(new Point(2.01, 1.0)));
        assertTrue(rectangle.contains(new Rect(0.5, 0.5, 1.5, 1.5)));
        assertFalse(rectangle.contains(new Rect(-0.5, 0.5, 1.5, 1.5)));
    }
    @Test
    public void shouldDetectIntersectionsIncludingBoundaries() {
        Rect rectangle=new Rect(0.0, 0.0, 2.0, 2.0);
        assertTrue(rectangle.intersects(new Rect(1.0, 1.0, 3.0, 3.0)));
        assertTrue(rectangle.intersects(new Rect(2.0, 0.0, 3.0, 1.0)));
        assertFalse(rectangle.intersects(new Rect(2.01, 0.0, 3.0, 1.0)));
    }
    @Test
    public void shouldCreateFromPointsAndUnion() {
        Rect rectangle=Rect.fromPoints(new Point(4.0, 5.0), new Point(-1.0, -2.0));
        Rect union=rectangle.union(new Rect(-3.0, 0.0, 1.0, 6.0));
        assertEquals(new Rect(-1.0, -2.0, 4.0, 5.0), rectangle);
        assertEquals(new Rect(-3.0, -2.0, 4.0, 6.0), union);
    }
    @Test
    public void shouldRejectInvalidBoundsAndArguments() {
        double[] invalidValues={Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY};
        for (double value : invalidValues) {
            try {
                new Rect(value, 0.0, 1.0, 1.0);
                fail("Expected non-finite minimum x to be rejected");
            } catch (IllegalArgumentException exception) {
                assertEquals(true, exception.getMessage().length()>0);
            }
        }
        try {
            new Rect(2.0, 0.0, 1.0, 1.0);
            fail("Expected reversed horizontal bounds to be rejected");
        } catch (IllegalArgumentException exception) {
            assertEquals(true, exception.getMessage().length()>0);
        }
        try {
            new Rect(0.0, 2.0, 1.0, 1.0);
            fail("Expected reversed vertical bounds to be rejected");
        } catch (IllegalArgumentException exception) {
            assertEquals(true, exception.getMessage().length()>0);
        }
        try {
            new Rect(0.0, 0.0, 1.0, 1.0).contains((Point)null);
            fail("Expected null point to be rejected");
        } catch (IllegalArgumentException exception) {
            assertEquals(true, exception.getMessage().length()>0);
        }
    }
    @Test
    public void shouldUseValueEqualityHashCodeAndString() {
        Rect first=new Rect(0.0, 1.0, 2.0, 3.0);
        Rect second=new Rect(0.0, 1.0, 2.0, 3.0);
        assertEquals(first, second);
        assertEquals(first.hashCode(), second.hashCode());
        assertNotEquals(first, new Rect(0.0, 1.0, 2.0, 4.0));
        assertEquals("Rect{minX=0.0, minY=1.0, maxX=2.0, maxY=3.0}", first.toString());
    }
}
