package com.vectorium.core.geometry;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
public final class MarchingSquaresTest {
    @Test
    public void shouldAssertEdgesForEveryNonAmbiguousMask() {
        int[] firstEdges={-1, 3, 0, 3, 1, -1, 0, 0, 2, 2, -1, 1, 0, 0, 0, -1};
        int[] secondEdges={-1, 0, 1, 1, 2, -1, 2, 2, 3, 0, -1, 2, 2, 1, 3, -1};
        for (int mask=0;mask<16;mask++) {
            if (mask==5||mask==10) {
                continue;
            }
            double[] field={(mask&1)!=0?1.0:0.0, (mask&2)!=0?1.0:0.0, (mask&4)!=0?1.0:0.0, (mask&8)!=0?1.0:0.0};
            double[] output=new double[8];
            for (int index=0;index<output.length;index++) {
                output[index]=9.0;
            }
            int count=MarchingSquares.appendCell(field, 2, 2, 0, 0, 0.5, output, 0);
            if (mask==0||mask==15) {
                assertEquals(0, count);
                for (int index=0;index<output.length;index++) {
                    assertEquals(9.0, output[index], 0.0);
                }
            } else {
                assertEquals(4, count);
                assertPoint(output[0], output[1], firstEdges[mask]);
                assertPoint(output[2], output[3], secondEdges[mask]);
            }
        }
    }
    @Test
    public void shouldKeepBothSaddleBranchesAndEqualityInside() {
        double[] caseFiveOutside={1.0, -1.0, 1.0, -1.0};
        double[] output=new double[8];
        assertEquals(8, MarchingSquares.appendCell(caseFiveOutside, 2, 2, 0, 0, 0.5, output, 0));
        assertEquals(0.25, output[0], 0.0);
        assertEquals(0.0, output[1], 0.0);
        assertEquals(0.0, output[2], 0.0);
        assertEquals(0.25, output[3], 0.0);
        assertEquals(1.0, output[4], 0.0);
        assertEquals(0.75, output[5], 0.0);
        assertEquals(0.75, output[6], 0.0);
        assertEquals(1.0, output[7], 0.0);
        double[] caseTenOutside={-1.0, 1.0, -1.0, 1.0};
        assertEquals(8, MarchingSquares.appendCell(caseTenOutside, 2, 2, 0, 0, 0.5, output, 0));
        assertEquals(0.75, output[0], 0.0);
        assertEquals(0.0, output[1], 0.0);
        assertEquals(1.0, output[2], 0.0);
        assertEquals(0.25, output[3], 0.0);
        assertEquals(0.0, output[4], 0.0);
        assertEquals(0.75, output[5], 0.0);
        assertEquals(0.25, output[6], 0.0);
        assertEquals(1.0, output[7], 0.0);
        double[] equality={0.5, 0.0, 0.0, 0.0};
        assertEquals(4, MarchingSquares.appendCell(equality, 2, 2, 0, 0, 0.5, output, 0));
        double[] caseFiveInside={1.0, 0.0, 1.0, 0.0};
        assertEquals(8, MarchingSquares.appendCell(caseFiveInside, 2, 2, 0, 0, 0.5, output, 0));
        assertEquals(0.5, output[0], 0.0);
        assertEquals(0.5, output[3], 0.0);
        double[] caseTenInside={0.0, 1.0, 0.0, 1.0};
        assertEquals(8, MarchingSquares.appendCell(caseTenInside, 2, 2, 0, 0, 0.5, output, 0));
        assertEquals(0.5, output[0], 0.0);
        assertEquals(0.5, output[3], 0.0);
    }
    @Test
    public void shouldValidateDimensionsOutputAndOffset() {
        double[] field={1.0, 0.0, 0.0, 0.0};
        double[] output={9.0, 9.0, 9.0, 9.0, 9.0, 9.0};
        assertEquals(4, MarchingSquares.appendCell(field, 2, 2, 0, 0, 0.5, output, 2));
        assertEquals(9.0, output[0], 0.0);
        assertEquals(9.0, output[1], 0.0);
        assertEquals(0.0, output[2], 0.0);
        assertEquals(0.5, output[3], 0.0);
        try {
            MarchingSquares.appendCell(field, 1, 2, 0, 0, 0.5, output, 0);
            fail("Expected width below two to be rejected");
        } catch (IllegalArgumentException exception) {
            assertTrue(exception.getMessage().length()>0);
        }
        try {
            MarchingSquares.appendCell(field, 2, 1, 0, 0, 0.5, output, 0);
            fail("Expected height below two to be rejected");
        } catch (IllegalArgumentException exception) {
            assertTrue(exception.getMessage().length()>0);
        }
        try {
            MarchingSquares.appendCell(field, 2, 2, 0, 0, 0.5, new double[3], 0);
            fail("Expected exhausted output to be rejected");
        } catch (IllegalArgumentException exception) {
            assertTrue(exception.getMessage().length()>0);
        }
    }
    private static void assertPoint(double x, double y, int edge) {
        if (edge==0) {
            assertEquals(0.5, x, 0.0);
            assertEquals(0.0, y, 0.0);
        } else if (edge==1) {
            assertEquals(1.0, x, 0.0);
            assertEquals(0.5, y, 0.0);
        } else if (edge==2) {
            assertEquals(0.5, x, 0.0);
            assertEquals(1.0, y, 0.0);
        } else {
            assertEquals(0.0, x, 0.0);
            assertEquals(0.5, y, 0.0);
        }
    }
}
