package com.anlyflad.core.model;
public final class Metrics {
    private final int pathCount;
    private final int nodeCount;
    private final Rect bounds;
    private final double area;
    private final long sourceBytes;
    private final long outputBytes;
    private final long lastRunNanos;
    public Metrics(int pathCount, int nodeCount, Rect bounds, double area, long sourceBytes, long outputBytes, long lastRunNanos) {
        if (pathCount<0) {
            throw new IllegalArgumentException("pathCount must be nonnegative");
        }
        if (nodeCount<0) {
            throw new IllegalArgumentException("nodeCount must be nonnegative");
        }
        if (bounds==null) {
            throw new IllegalArgumentException("bounds must not be null");
        }
        if (!Double.isFinite(area)||area<0.0) {
            throw new IllegalArgumentException("area must be finite and nonnegative");
        }
        if (sourceBytes<0L) {
            throw new IllegalArgumentException("sourceBytes must be nonnegative");
        }
        if (outputBytes<0L) {
            throw new IllegalArgumentException("outputBytes must be nonnegative");
        }
        if (lastRunNanos<0L) {
            throw new IllegalArgumentException("lastRunNanos must be nonnegative");
        }
        this.pathCount=pathCount;
        this.nodeCount=nodeCount;
        this.bounds=bounds;
        this.area=area;
        this.sourceBytes=sourceBytes;
        this.outputBytes=outputBytes;
        this.lastRunNanos=lastRunNanos;
    }
    public int getPathCount() {
        return pathCount;
    }
    public int getNodeCount() {
        return nodeCount;
    }
    public Rect getBounds() {
        return bounds;
    }
    public double getArea() {
        return area;
    }
    public long getSourceBytes() {
        return sourceBytes;
    }
    public long getOutputBytes() {
        return outputBytes;
    }
    public long getLastRunNanos() {
        return lastRunNanos;
    }
    @Override
    public boolean equals(Object other) {
        if (this==other) {
            return true;
        }
        if (other==null||getClass()!=other.getClass()) {
            return false;
        }
        Metrics metrics=(Metrics)other;
        return pathCount==metrics.pathCount&&nodeCount==metrics.nodeCount&&Double.doubleToLongBits(area)==Double.doubleToLongBits(metrics.area)&&sourceBytes==metrics.sourceBytes&&outputBytes==metrics.outputBytes&&lastRunNanos==metrics.lastRunNanos&&bounds.equals(metrics.bounds);
    }
    @Override
    public int hashCode() {
        int result=17;
        result=31*result+pathCount;
        result=31*result+nodeCount;
        result=31*result+bounds.hashCode();
        result=31*result+longBits(area);
        result=31*result+(int)(sourceBytes^(sourceBytes>>>32));
        result=31*result+(int)(outputBytes^(outputBytes>>>32));
        result=31*result+(int)(lastRunNanos^(lastRunNanos>>>32));
        return result;
    }
    @Override
    public String toString() {
        return "Metrics{pathCount="+pathCount+", nodeCount="+nodeCount+", bounds="+bounds+", area="+area+", sourceBytes="+sourceBytes+", outputBytes="+outputBytes+", lastRunNanos="+lastRunNanos+"}";
    }
    private static int longBits(double value) {
        long bits=Double.doubleToLongBits(value);
        return (int)(bits^(bits>>>32));
    }
}
