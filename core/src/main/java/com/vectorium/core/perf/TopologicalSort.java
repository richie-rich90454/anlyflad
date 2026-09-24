package com.vectorium.core.perf;
public final class TopologicalSort {
    private TopologicalSort() {
    }
    public static int sort(int nodeCount, int[] edgeSources, int[] edgeTargets, int[] output, int[] indegree, int[] queue, int[] heads, int[] nextEdges) {
        if (nodeCount<0) {
            throw new IllegalArgumentException("nodeCount must be nonnegative");
        }
        if (edgeSources==null||edgeTargets==null||edgeSources.length!=edgeTargets.length) {
            throw new IllegalArgumentException("edge arrays must have equal lengths");
        }
        if (output==null||output.length!=nodeCount) {
            throw new IllegalArgumentException("output must have one entry per node");
        }
        if (indegree==null||indegree.length!=nodeCount) {
            throw new IllegalArgumentException("indegree must have one entry per node");
        }
        if (queue==null||queue.length!=nodeCount) {
            throw new IllegalArgumentException("queue must have one entry per node");
        }
        if (heads==null||heads.length!=nodeCount) {
            throw new IllegalArgumentException("heads must have one entry per node");
        }
        if (nextEdges==null||nextEdges.length!=edgeSources.length) {
            throw new IllegalArgumentException("nextEdges must have one entry per edge");
        }
        for (int node=0;node<nodeCount;node++) {
            indegree[node]=0;
            heads[node]=-1;
        }
        for (int edge=0;edge<edgeSources.length;edge++) {
            int source=edgeSources[edge];
            int target=edgeTargets[edge];
            validateNode(source, nodeCount);
            validateNode(target, nodeCount);
            if (indegree[target]==Integer.MAX_VALUE) {
                throw new IllegalArgumentException("indegree overflow");
            }
            indegree[target]++;
        }
        for (int edge=edgeSources.length-1;edge>=0;edge--) {
            int source=edgeSources[edge];
            nextEdges[edge]=heads[source];
            heads[source]=edge;
        }
        int queueHead=0;
        int queueTail=0;
        for (int node=0;node<nodeCount;node++) {
            if (indegree[node]==0) {
                queue[queueTail]=node;
                queueTail++;
            }
        }
        int emitted=0;
        while (queueHead<queueTail) {
            int node=queue[queueHead];
            queueHead++;
            output[emitted]=node;
            emitted++;
            int edge=heads[node];
            while (edge!=-1) {
                int target=edgeTargets[edge];
                indegree[target]--;
                if (indegree[target]==0) {
                    queue[queueTail]=target;
                    queueTail++;
                }
                edge=nextEdges[edge];
            }
        }
        return emitted;
    }
    private static void validateNode(int node, int nodeCount) {
        if (node<0||node>=nodeCount) {
            throw new IllegalArgumentException("edge endpoint is outside the node range");
        }
    }
}
