package com.vectorium.core.perf;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
public final class MortonCodesTest {
    @Test
    public void shouldPackAndUnpackAllCoordinateBits() {
        int[] values={Integer.MIN_VALUE, -1, 0, 1, Integer.MAX_VALUE};
        for (int index=0;index<values.length;index++) {
            long code=MortonCodes.pack(values[index], values[values.length-index-1]);
            assertEquals(values[index], MortonCodes.unpackX(code));
            assertEquals(values[values.length-index-1], MortonCodes.unpackY(code));
        }
    }
    @Test
    public void shouldUnsignedSortPrefixAndSearchCoordinates() {
        long zero=MortonCodes.pack(0, 0);
        long minX=MortonCodes.pack(Integer.MIN_VALUE, 0);
        long minY=MortonCodes.pack(0, Integer.MIN_VALUE);
        long maxBoth=MortonCodes.pack(Integer.MAX_VALUE, Integer.MAX_VALUE);
        long[] values={maxBoth, minY, zero, minX, zero, 77L};
        long[] scratch=new long[values.length];
        assertEquals(5, MortonCodes.sort(values, 5, scratch));
        assertEquals(zero, values[0]);
        assertEquals(zero, values[1]);
        assertEquals(maxBoth, values[2]);
        assertEquals(minX, values[3]);
        assertEquals(minY, values[4]);
        assertEquals(77L, values[5]);
        assertTrue(Long.compareUnsigned(values[2], values[3])<0);
        assertTrue(Long.compareUnsigned(values[3], values[4])<0);
        assertEquals(0, MortonCodes.search(values, 5, 0, 0));
        assertEquals(4, MortonCodes.search(values, 5, 0, Integer.MIN_VALUE));
        assertEquals(3, MortonCodes.search(values, 5, Integer.MIN_VALUE, 0));
        assertEquals(2, MortonCodes.search(values, 5, Integer.MAX_VALUE, Integer.MAX_VALUE));
        assertEquals(-1, MortonCodes.search(values, 5, 1, 2));
    }
    @Test
    public void shouldValidatePrefixAndScratch() {
        long[] values={1L, 2L};
        try {
            MortonCodes.sort(values, 3, new long[3]);
            fail("Expected invalid prefix length to be rejected");
        } catch (IllegalArgumentException exception) {
            assertTrue(exception.getMessage().length()>0);
        }
        try {
            MortonCodes.sort(values, 2, new long[1]);
            fail("Expected short scratch to be rejected");
        } catch (IllegalArgumentException exception) {
            assertTrue(exception.getMessage().length()>0);
        }
    }
}
