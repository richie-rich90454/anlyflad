package com.anlyflad.core.perf;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
public final class TopologicalSortTest {
    @Test
    public void shouldTraverseOutgoingEdgesInInputOrder() {
        int[] edgeSources={0, 0, 1};
        int[] edgeTargets={2, 3, 4};
        int[] output=new int[5];
        int[] indegree=new int[5];
        int[] queue=new int[5];
        int[] heads=new int[5];
        int[] nextEdges=new int[3];
        assertEquals(5, TopologicalSort.sort(5, edgeSources, edgeTargets, output, indegree, queue, heads, nextEdges));
        assertArrayEquals(new int[]{0, 1, 2, 3, 4}, output);
    }
    @Test
    public void shouldHandleDuplicateEdges() {
        int[] edgeSources={0, 0, 1};
        int[] edgeTargets={1, 1, 2};
        int[] output=new int[3];
        int[] indegree=new int[3];
        int[] queue=new int[3];
        int[] heads=new int[3];
        int[] nextEdges=new int[3];
        assertEquals(3, TopologicalSort.sort(3, edgeSources, edgeTargets, output, indegree, queue, heads, nextEdges));
        assertArrayEquals(new int[]{0, 1, 2}, output);
    }
    @Test
    public void shouldReturnZeroForSelfCycle() {
        int[] edgeSources={0};
        int[] edgeTargets={0};
        int[] output=new int[1];
        int[] indegree=new int[1];
        int[] queue=new int[1];
        int[] heads=new int[1];
        int[] nextEdges=new int[1];
        assertEquals(0, TopologicalSort.sort(1, edgeSources, edgeTargets, output, indegree, queue, heads, nextEdges));
    }
    @Test
    public void shouldReturnZeroForMultipleCycles() {
        int[] edgeSources={0, 1, 2, 3, 4, 5};
        int[] edgeTargets={1, 2, 0, 4, 5, 3};
        int[] output=new int[6];
        int[] indegree=new int[6];
        int[] queue=new int[6];
        int[] heads=new int[6];
        int[] nextEdges=new int[6];
        assertEquals(0, TopologicalSort.sort(6, edgeSources, edgeTargets, output, indegree, queue, heads, nextEdges));
    }
    @Test
    public void shouldHandleEmptyGraphAndValidateScratch() {
        int[] output=new int[0];
        int[] indegree=new int[0];
        int[] queue=new int[0];
        int[] heads=new int[0];
        int[] nextEdges=new int[0];
        assertEquals(0, TopologicalSort.sort(0, new int[0], new int[0], output, indegree, queue, heads, nextEdges));
        try {
            TopologicalSort.sort(1, new int[0], new int[0], new int[1], new int[1], new int[1], new int[0], new int[0]);
            fail("Expected exact heads coverage to be required");
        } catch (IllegalArgumentException exception) {
            assertTrue(exception.getMessage().length()>0);
        }
        try {
            TopologicalSort.sort(1, new int[]{0}, new int[]{1}, new int[1], new int[1], new int[1], new int[1], new int[0]);
            fail("Expected exact nextEdges coverage to be required");
        } catch (IllegalArgumentException exception) {
            assertTrue(exception.getMessage().length()>0);
        }
    }
    @Test
    public void shouldRejectInvalidEndpoint() {
        try {
            TopologicalSort.sort(1, new int[]{0}, new int[]{1}, new int[1], new int[1], new int[1], new int[1], new int[1]);
            fail("Expected invalid endpoint to be rejected");
        } catch (IllegalArgumentException exception) {
            assertTrue(exception.getMessage().length()>0);
        }
    }
}
