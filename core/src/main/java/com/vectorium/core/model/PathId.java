package com.vectorium.core.model;
public final class PathId {
    private final int value;
    public PathId(int value) {
        if (value<0) {
            throw new IllegalArgumentException("Path id must be nonnegative");
        }
        this.value=value;
    }
    public static PathId of(int value) {
        return new PathId(value);
    }
    public static PathId zero() {
        return new PathId(0);
    }
    public int getValue() {
        return value;
    }
    public PathId next() {
        return new PathId(value+1);
    }
    @Override
    public boolean equals(Object other) {
        if (this==other) {
            return true;
        }
        if (other==null||getClass()!=other.getClass()) {
            return false;
        }
        PathId pathId=(PathId)other;
        return value==pathId.value;
    }
    @Override
    public int hashCode() {
        return 17+value;
    }
    @Override
    public String toString() {
        return "PathId{value="+value+"}";
    }
}
