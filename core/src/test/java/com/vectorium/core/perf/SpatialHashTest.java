package com.vectorium.core.perf;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
public final class SpatialHashTest {
    @Test
    public void shouldInsertPointAndQueryCellsInTraversalOrder() {
        SpatialHash hash=new SpatialHash(0.0, 0.0, 4.0, 4.0, 2, 8);
        assertTrue(hash.insert(7, 1.0, 1.0));
        assertTrue(hash.insert(8, 3.0, 3.0));
        int[] output=new int[4];
        assertEquals(1, hash.query(1.0, 1.0, output));
        assertEquals(7, output[0]);
        assertEquals(1, hash.query(3.0, 3.0, output));
        assertEquals(8, output[0]);
    }
    @Test
    public void shouldClampPartialOverlapAndRepeatAcrossCells() {
        SpatialHash hash=new SpatialHash(0.0, 0.0, 4.0, 4.0, 2, 8);
        assertTrue(hash.insert(11, -1.0, -1.0, 2.0, 2.0));
        int[] output=new int[8];
        assertEquals(4, hash.query(0.0, 0.0, 4.0, 4.0, output));
        for (int index=0;index<4;index++) {
            assertEquals(11, output[index]);
        }
        assertEquals(0, hash.query(5.0, 5.0, 6.0, 6.0, output));
    }
    @Test
    public void shouldBeAtomicWhenCapacityIsInsufficientAndClearReusableStorage() {
        SpatialHash hash=new SpatialHash(0.0, 0.0, 4.0, 4.0, 2, 1);
        assertEquals(false, hash.insert(1, 0.0, 0.0, 4.0, 4.0));
        assertEquals(0, hash.getUsedCapacity());
        assertTrue(hash.insert(2, 1.0, 1.0));
        int[] output=new int[1];
        assertEquals(1, hash.query(1.0, 1.0, 1.0, 1.0, output));
        hash.clear();
        assertEquals(0, hash.getUsedCapacity());
        assertEquals(0, hash.query(0.0, 0.0, 4.0, 4.0, output));
        assertTrue(hash.insert(3, 1.0, 1.0));
    }
    @Test
    public void shouldRejectNegativeItemId() {
        SpatialHash hash=new SpatialHash(0.0, 0.0, 2.0, 2.0, 1, 1);
        try {
            hash.insert(-1, 1.0, 1.0);
            fail("Expected negative itemId to be rejected");
        } catch (IllegalArgumentException exception) {
            assertTrue(exception.getMessage().length()>0);
        }
    }
    @Test
    public void shouldRejectInvalidBoundsAndExhaustedOutput() {
        try {
            new SpatialHash(0.0, 0.0, 0.0, 1.0, 1, 1);
            fail("Expected zero-width bounds to be rejected");
        } catch (IllegalArgumentException exception) {
            assertTrue(exception.getMessage().length()>0);
        }
        SpatialHash hash=new SpatialHash(0.0, 0.0, 2.0, 2.0, 1, 1);
        assertTrue(hash.insert(1, 0.0, 0.0, 2.0, 2.0));
        try {
            hash.query(0.0, 0.0, 2.0, 2.0, new int[0]);
            fail("Expected exhausted output to be rejected");
        } catch (IllegalArgumentException exception) {
            assertTrue(exception.getMessage().length()>0);
        }
    }
}
