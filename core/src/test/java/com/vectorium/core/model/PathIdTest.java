package com.vectorium.core.model;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.fail;
public final class PathIdTest {
    @Test
    public void shouldCreateZeroBasedIdsAndAdvance() {
        PathId zero=PathId.zero();
        PathId first=PathId.of(1);
        assertEquals(0, zero.getValue());
        assertEquals(1, first.getValue());
        assertEquals(2, first.next().getValue());
    }
    @Test
    public void shouldRejectNegativeIds() {
        try {
            PathId.of(-1);
            fail("Expected negative id to be rejected");
        } catch (IllegalArgumentException exception) {
            assertEquals(true, exception.getMessage().length()>0);
        }
    }
    @Test
    public void shouldRejectOverflowWhenAdvancingMaximumId() {
        try {
            PathId.of(Integer.MAX_VALUE).next();
            fail("Expected overflowing next id to be rejected");
        } catch (IllegalArgumentException exception) {
            assertEquals(true, exception.getMessage().length()>0);
        }
    }
    @Test
    public void shouldUseValueEqualityHashCodeAndString() {
        PathId first=new PathId(7);
        PathId second=PathId.of(7);
        assertEquals(first, second);
        assertEquals(first.hashCode(), second.hashCode());
        assertNotEquals(first, PathId.of(8));
        assertEquals("PathId{value=7}", first.toString());
    }
}
