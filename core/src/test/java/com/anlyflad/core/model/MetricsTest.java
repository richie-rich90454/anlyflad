package com.anlyflad.core.model;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.fail;
public final class MetricsTest {
    @Test
    public void shouldStoreSummaryValues() {
        Rect bounds=new Rect(-1.0, -2.0, 5.0, 8.0);
        Metrics metrics=new Metrics(3, 42, bounds, 123.5, 1024L, 2048L, 5000000L);
        assertEquals(3, metrics.getPathCount());
        assertEquals(42, metrics.getNodeCount());
        assertEquals(bounds, metrics.getBounds());
        assertEquals(123.5, metrics.getArea());
        assertEquals(1024L, metrics.getSourceBytes());
        assertEquals(2048L, metrics.getOutputBytes());
        assertEquals(5000000L, metrics.getLastRunNanos());
    }
    @Test
    public void shouldRejectInvalidSummaryValues() {
        int[] invalidCounts={-1, Integer.MIN_VALUE};
        for (int count : invalidCounts) {
            try {
                new Metrics(count, 0, new Rect(0.0, 0.0, 0.0, 0.0), 0.0, 0L, 0L, 0L);
                fail("Expected negative path count to be rejected");
            } catch (IllegalArgumentException exception) {
                assertEquals(true, exception.getMessage().length()>0);
            }
            try {
                new Metrics(0, count, new Rect(0.0, 0.0, 0.0, 0.0), 0.0, 0L, 0L, 0L);
                fail("Expected negative node count to be rejected");
            } catch (IllegalArgumentException exception) {
                assertEquals(true, exception.getMessage().length()>0);
            }
        }
        try {
            new Metrics(0, 0, null, 0.0, 0L, 0L, 0L);
            fail("Expected null bounds to be rejected");
        } catch (IllegalArgumentException exception) {
            assertEquals(true, exception.getMessage().length()>0);
        }
        double[] invalidAreas={-0.01, Double.NaN, Double.POSITIVE_INFINITY};
        for (double area : invalidAreas) {
            try {
                new Metrics(0, 0, new Rect(0.0, 0.0, 0.0, 0.0), area, 0L, 0L, 0L);
                fail("Expected invalid area to be rejected");
            } catch (IllegalArgumentException exception) {
                assertEquals(true, exception.getMessage().length()>0);
            }
        }
        long[] invalidLongCounts={-1L, Long.MIN_VALUE};
        for (long count : invalidLongCounts) {
            try {
                new Metrics(0, 0, new Rect(0.0, 0.0, 0.0, 0.0), 0.0, count, 0L, 0L);
                fail("Expected negative source bytes to be rejected");
            } catch (IllegalArgumentException exception) {
                assertEquals(true, exception.getMessage().length()>0);
            }
            try {
                new Metrics(0, 0, new Rect(0.0, 0.0, 0.0, 0.0), 0.0, 0L, count, 0L);
                fail("Expected negative output bytes to be rejected");
            } catch (IllegalArgumentException exception) {
                assertEquals(true, exception.getMessage().length()>0);
            }
            try {
                new Metrics(0, 0, new Rect(0.0, 0.0, 0.0, 0.0), 0.0, 0L, 0L, count);
                fail("Expected negative run time to be rejected");
            } catch (IllegalArgumentException exception) {
                assertEquals(true, exception.getMessage().length()>0);
            }
        }
    }
    @Test
    public void shouldUseValueEqualityHashCodeAndString() {
        Rect bounds=new Rect(0.0, 0.0, 2.0, 3.0);
        Metrics first=new Metrics(1, 4, bounds, 6.0, 10L, 20L, 30L);
        Metrics second=new Metrics(1, 4, bounds, 6.0, 10L, 20L, 30L);
        assertEquals(first, second);
        assertEquals(first.hashCode(), second.hashCode());
        assertNotEquals(first, new Metrics(1, 5, bounds, 6.0, 10L, 20L, 30L));
        assertEquals("Metrics{pathCount=1, nodeCount=4, bounds=Rect{minX=0.0, minY=0.0, maxX=2.0, maxY=3.0}, area=6.0, sourceBytes=10, outputBytes=20, lastRunNanos=30}", first.toString());
    }
}
