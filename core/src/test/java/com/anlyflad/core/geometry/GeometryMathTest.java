package com.anlyflad.core.geometry;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
public final class GeometryMathTest {
    @Test
    public void shouldCalculateVectorOperationsIntoCallerOutput() {
        double[] output=new double[2];
        GeometryMath.add(new double[]{1.5, -2.0}, new double[]{2.0, 4.0}, output);
        assertEquals(3.5, output[0], 0.0);
        assertEquals(2.0, output[1], 0.0);
        GeometryMath.subtract(new double[]{3.0, 5.0}, new double[]{1.0, 2.0}, output);
        assertEquals(2.0, output[0], 0.0);
        assertEquals(3.0, output[1], 0.0);
        GeometryMath.scale(new double[]{2.0, -3.0}, 2.5, output);
        assertEquals(5.0, output[0], 0.0);
        assertEquals(-7.5, output[1], 0.0);
        GeometryMath.lerp(new double[]{0.0, 10.0}, new double[]{4.0, 2.0}, 0.25, output);
        assertEquals(1.0, output[0], 0.0);
        assertEquals(8.0, output[1], 0.0);
    }
    @Test
    public void shouldCalculateScalarGeometryOperations() {
        assertEquals(11.0, GeometryMath.dot(new double[]{1.0, 2.0}, new double[]{3.0, 4.0}), 0.0);
        assertEquals(-2.0, GeometryMath.cross(new double[]{1.0, 2.0}, new double[]{3.0, 4.0}), 0.0);
        assertEquals(25.0, GeometryMath.distanceSquared(new double[]{1.0, 2.0}, new double[]{4.0, 6.0}), 0.0);
        assertEquals(0.25, GeometryMath.pointSegmentDistanceSquared(0.5, 0.5, 0.0, 0.0, 1.0, 0.0), 0.0);
        assertEquals(1.0, GeometryMath.pointSegmentDistanceSquared(2.0, 0.0, 0.0, 0.0, 1.0, 0.0), 0.0);
        assertEquals(1.0, GeometryMath.pointSegmentDistanceSquared(0.0, 1.0, 0.0, 0.0, 0.0, 0.0), 0.0);
    }
    @Test
    public void shouldCalculateAreaAndContainmentWithBoundaryAndEvenOddRules() {
        double[] square={0.0, 0.0, 4.0, 0.0, 4.0, 4.0, 0.0, 4.0};
        assertEquals(32.0, GeometryMath.shoelace2(square), 0.0);
        assertTrue(GeometryMath.contains(square, 2.0, 2.0));
        assertTrue(GeometryMath.contains(square, 2.0, 0.0));
        assertTrue(GeometryMath.contains(square, 0.0, 2.0));
        assertFalse(GeometryMath.contains(square, 5.0, 2.0));
        double[] notched={0.0, 0.0, 4.0, 0.0, 4.0, 4.0, 2.0, 2.0, 0.0, 4.0};
        assertTrue(GeometryMath.contains(notched, 2.0, 1.0));
        assertFalse(GeometryMath.contains(notched, 2.0, 3.0));
        assertFalse(GeometryMath.contains(new double[]{0.0, 0.0, 1.0, 1.0}, 0.0, 0.0));
    }
    @Test
    public void shouldRejectInvalidGeometryInputsAndResults() {
        double[] output=new double[2];
        try {
            GeometryMath.add(new double[]{Double.NaN, 0.0}, new double[]{1.0, 1.0}, output);
            fail("Expected nonfinite input to be rejected");
        } catch (IllegalArgumentException exception) {
            assertTrue(exception.getMessage().length()>0);
        }
        try {
            GeometryMath.scale(new double[]{Double.MAX_VALUE, 0.0}, 2.0, output);
            fail("Expected nonfinite result to be rejected");
        } catch (IllegalArgumentException exception) {
            assertTrue(exception.getMessage().length()>0);
        }
        try {
            GeometryMath.contains(new double[]{0.0, 0.0, Double.POSITIVE_INFINITY, 1.0}, 0.0, 0.0);
            fail("Expected nonfinite polygon to be rejected");
        } catch (IllegalArgumentException exception) {
            assertTrue(exception.getMessage().length()>0);
        }
    }
}
