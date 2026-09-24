package com.vectorium.core.geometry;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
public final class DouglasPeuckerTest {
    @Test
    public void shouldSimplifyOpenPathAndKeepEndpoints() {
        double[] xy={0.0, 0.0, 1.0, 1.0, 2.0, 0.0};
        boolean[] keep=new boolean[3];
        int[] stack=new int[6];
        int[] output=new int[3];
        int count=DouglasPeucker.simplify(xy, false, 2.0, keep, stack, output);
        assertEquals(2, count);
        assertEquals(0, output[0]);
        assertEquals(2, output[1]);
        assertTrue(keep[0]);
        assertFalse(keep[1]);
        assertTrue(keep[2]);
    }
    @Test
    public void shouldRemoveEqualityAndUseFirstFarthest() {
        double[] xy={0.0, 0.0, 1.0, 1.0, 2.0, 0.0};
        boolean[] keep=new boolean[3];
        int[] stack=new int[6];
        int[] output=new int[3];
        assertEquals(2, DouglasPeucker.simplify(xy, false, 1.0, keep, stack, output));
        assertEquals(0, output[0]);
        assertEquals(2, output[1]);
        double[] tied={0.0, 0.0, 1.0, 1.0, 3.0, 1.0, 4.0, 0.0};
        keep=new boolean[4];
        stack=new int[8];
        output=new int[4];
        assertEquals(3, DouglasPeucker.simplify(tied, false, 0.8, keep, stack, output));
        assertEquals(0, output[0]);
        assertEquals(1, output[1]);
        assertEquals(3, output[2]);
    }
    @Test
    public void shouldRemovePointAtExactTolerance() {
        double[] xy={0.0, 0.0, 1.0, 1.0, 2.0, 0.0};
        boolean[] keep=new boolean[3];
        int[] stack=new int[6];
        int[] output=new int[3];
        assertEquals(2, DouglasPeucker.simplify(xy, false, 1.0, keep, stack, output));
        assertFalse(keep[1]);
    }
    @Test
    public void shouldSimplifyBothClosedChainsInAscendingOrder() {
        double[] xy={2.0, 2.0, 0.0, 0.0, 1.0, 0.0, 2.0, 1.0, 0.0, 2.0};
        boolean[] keep=new boolean[5];
        int[] stack=new int[10];
        int[] output=new int[5];
        int count=DouglasPeucker.simplify(xy, true, 0.25, keep, stack, output);
        assertTrue(count>=2);
        for (int index=1;index<count;index++) {
            assertTrue(output[index-1]<output[index]);
        }
        assertEquals(0, output[0]);
        assertEquals(4, output[count-1]);
    }
    @Test
    public void shouldCollapseCoincidentClosedRingAndValidateScratch() {
        double[] xy={1.0, 1.0, 1.0, 1.0, 1.0, 1.0};
        boolean[] keep=new boolean[3];
        int[] stack=new int[6];
        int[] output=new int[3];
        assertEquals(1, DouglasPeucker.simplify(xy, true, 0.0, keep, stack, output));
        assertEquals(0, output[0]);
        try {
            DouglasPeucker.simplify(new double[]{0.0, 0.0}, false, 0.0, new boolean[1], new int[1], new int[1]);
            fail("Expected exact stack size to be required");
        } catch (IllegalArgumentException exception) {
            assertTrue(exception.getMessage().length()>0);
        }
        try {
            DouglasPeucker.simplify(new double[]{0.0, 0.0}, false, Double.NaN, new boolean[1], new int[2], new int[1]);
            fail("Expected invalid tolerance to be rejected");
        } catch (IllegalArgumentException exception) {
            assertTrue(exception.getMessage().length()>0);
        }
    }
}
