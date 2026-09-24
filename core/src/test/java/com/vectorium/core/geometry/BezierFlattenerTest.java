package com.vectorium.core.geometry;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
public final class BezierFlattenerTest {
    @Test
    public void shouldFlattenDegenerateCurveAtFixedUniformSteps() {
        BezierFlattener flattener=new BezierFlattener(4);
        double[] workspace=flattener.getOutputCoordinates();
        assertEquals(5, flattener.flatten(2.0, 3.0, 2.0, 3.0, 2.0, 3.0, 2.0, 3.0));
        assertEquals(5, flattener.getPointCount());
        assertSame(workspace, flattener.getOutputCoordinates());
        for (int index=0;index<5;index++) {
            assertEquals(2.0, flattener.getOutputCoordinates()[index*2], 0.0);
            assertEquals(3.0, flattener.getOutputCoordinates()[index*2+1], 0.0);
        }
    }
    @Test
    public void shouldUseSmallerStepCountAndKeepExactEndpoints() {
        BezierFlattener flattener=new BezierFlattener(4);
        assertEquals(3, flattener.flatten(0.0, 0.0, 0.0, 1.0, 1.0, 1.0, 1.0, 0.0, 2));
        assertEquals(0.0, flattener.getOutputCoordinates()[0], 0.0);
        assertEquals(0.5, flattener.getOutputCoordinates()[2], 0.0);
        assertEquals(0.75, flattener.getOutputCoordinates()[3], 0.0);
        assertEquals(1.0, flattener.getOutputCoordinates()[4], 0.0);
        assertEquals(0.0, flattener.getOutputCoordinates()[5], 0.0);
    }
    @Test
    public void shouldReuseWorkspaceAfterLargerThenSmallerCall() {
        BezierFlattener flattener=new BezierFlattener(4);
        double[] workspace=flattener.getOutputCoordinates();
        assertEquals(5, flattener.flatten(0.0, 0.0, 1.0, 1.0, 2.0, 2.0, 3.0, 3.0, 4));
        assertEquals(3, flattener.flatten(0.0, 0.0, 1.0, 1.0, 2.0, 2.0, 3.0, 3.0, 2));
        assertEquals(3, flattener.getPointCount());
        assertSame(workspace, flattener.getOutputCoordinates());
        assertEquals(0.0, flattener.getOutputCoordinates()[0], 0.0);
        assertEquals(3.0, flattener.getOutputCoordinates()[5], 0.0);
    }
    @Test
    public void shouldRejectInvalidInputAndStepCounts() {
        BezierFlattener flattener=new BezierFlattener(3);
        double[] workspace=flattener.getOutputCoordinates();
        flattener.flatten(0.0, 0.0, 1.0, 1.0, 2.0, 2.0, 3.0, 3.0);
        assertSame(workspace, flattener.getOutputCoordinates());
        try {
            flattener.flatten(Double.NaN, 0.0, 0.0, 0.0, 1.0, 1.0, 1.0, 1.0, 2);
            fail("Expected nonfinite control point to be rejected");
        } catch (IllegalArgumentException exception) {
            assertTrue(exception.getMessage().length()>0);
        }
        try {
            flattener.flatten(0.0, 0.0, 1.0, 1.0, 2.0, 2.0, 3.0, 3.0, 0);
            fail("Expected zero steps to be rejected");
        } catch (IllegalArgumentException exception) {
            assertTrue(exception.getMessage().length()>0);
        }
        try {
            flattener.flatten(0.0, 0.0, 1.0, 1.0, 2.0, 2.0, 3.0, 3.0, 4);
            fail("Expected excessive steps to be rejected");
        } catch (IllegalArgumentException exception) {
            assertTrue(exception.getMessage().length()>0);
        }
        try {
            new BezierFlattener(0);
            fail("Expected zero maxSteps to be rejected");
        } catch (IllegalArgumentException exception) {
            assertTrue(exception.getMessage().length()>0);
        }
    }
}
