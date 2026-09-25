package com.anlyflad.core.model;
public final class SvgOrigin implements Origin {
    private final String sourceName;
    public SvgOrigin(String sourceName) {
        if (sourceName==null||sourceName.trim().isEmpty()) {
            throw new IllegalArgumentException("sourceName must not be blank");
        }
        this.sourceName=sourceName;
    }
    @Override
    public String getSourceName() {
        return sourceName;
    }
    @Override
    public boolean isRaster() {
        return false;
    }
    @Override
    public boolean equals(Object other) {
        if (this==other) {
            return true;
        }
        if (other==null||getClass()!=other.getClass()) {
            return false;
        }
        SvgOrigin origin=(SvgOrigin)other;
        return sourceName.equals(origin.sourceName);
    }
    @Override
    public int hashCode() {
        return 17+sourceName.hashCode();
    }
    @Override
    public String toString() {
        return "SvgOrigin{sourceName="+sourceName+"}";
    }
}
