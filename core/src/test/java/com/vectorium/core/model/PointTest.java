package com.vectorium.core.model;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.fail;
public final class PointTest {
    @Test
    public void shouldStoreFiniteCoordinates() {
        Point point=new Point(12.5, -3.25);
        assertEquals(12.5, point.getX());
        assertEquals(-3.25, point.getY());
    }
    @Test
    public void shouldRejectNonFiniteCoordinates() {
        double[] invalidValues={Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY};
        for (double value : invalidValues) {
            try {
                new Point(value, 0.0);
                fail("Expected invalid x coordinate to be rejected");
            } catch (IllegalArgumentException exception) {
                assertEquals(true, exception.getMessage().length()>0);
            }
            try {
                new Point(0.0, value);
                fail("Expected invalid y coordinate to be rejected");
            } catch (IllegalArgumentException exception) {
                assertEquals(true, exception.getMessage().length()>0);
            }
        }
    }
    @Test
    public void shouldUseValueEqualityHashCodeAndString() {
        Point first=new Point(1.25, 2.5);
        Point second=new Point(1.25, 2.5);
        assertEquals(first, second);
        assertEquals(first.hashCode(), second.hashCode());
        assertNotEquals(first, new Point(1.25, 2.75));
        assertEquals("Point{x=1.25, y=2.5}", first.toString());
    }
}
