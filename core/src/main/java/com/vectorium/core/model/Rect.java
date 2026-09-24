package com.vectorium.core.model;
public final class Rect {
    private final double minX;
    private final double minY;
    private final double maxX;
    private final double maxY;
    public Rect(double minX, double minY, double maxX, double maxY) {
        if (!Double.isFinite(minX)) {
            throw new IllegalArgumentException("minX must be finite");
        }
        if (!Double.isFinite(minY)) {
            throw new IllegalArgumentException("minY must be finite");
        }
        if (!Double.isFinite(maxX)) {
            throw new IllegalArgumentException("maxX must be finite");
        }
        if (!Double.isFinite(maxY)) {
            throw new IllegalArgumentException("maxY must be finite");
        }
        if (minX>maxX) {
            throw new IllegalArgumentException("minX must not exceed maxX");
        }
        if (minY>maxY) {
            throw new IllegalArgumentException("minY must not exceed maxY");
        }
        this.minX=minX;
        this.minY=minY;
        this.maxX=maxX;
        this.maxY=maxY;
    }
    public static Rect fromPoints(Point first, Point second) {
        if (first==null) {
            throw new IllegalArgumentException("first point must not be null");
        }
        if (second==null) {
            throw new IllegalArgumentException("second point must not be null");
        }
        return new Rect(Math.min(first.getX(), second.getX()), Math.min(first.getY(), second.getY()), Math.max(first.getX(), second.getX()), Math.max(first.getY(), second.getY()));
    }
    public double getMinX() {
        return minX;
    }
    public double getMinY() {
        return minY;
    }
    public double getMaxX() {
        return maxX;
    }
    public double getMaxY() {
        return maxY;
    }
    public double getWidth() {
        return maxX-minX;
    }
    public double getHeight() {
        return maxY-minY;
    }
    public double getArea() {
        return getWidth()*getHeight();
    }
    public boolean contains(Point point) {
        if (point==null) {
            throw new IllegalArgumentException("point must not be null");
        }
        return point.getX()>=minX&&point.getX()<=maxX&&point.getY()>=minY&&point.getY()<=maxY;
    }
    public boolean contains(Rect rectangle) {
        if (rectangle==null) {
            throw new IllegalArgumentException("rectangle must not be null");
        }
        return rectangle.minX>=minX&&rectangle.maxX<=maxX&&rectangle.minY>=minY&&rectangle.maxY<=maxY;
    }
    public boolean intersects(Rect rectangle) {
        if (rectangle==null) {
            throw new IllegalArgumentException("rectangle must not be null");
        }
        return rectangle.maxX>=minX&&rectangle.minX<=maxX&&rectangle.maxY>=minY&&rectangle.minY<=maxY;
    }
    public Rect union(Rect rectangle) {
        if (rectangle==null) {
            throw new IllegalArgumentException("rectangle must not be null");
        }
        return new Rect(Math.min(minX, rectangle.minX), Math.min(minY, rectangle.minY), Math.max(maxX, rectangle.maxX), Math.max(maxY, rectangle.maxY));
    }
    @Override
    public boolean equals(Object other) {
        if (this==other) {
            return true;
        }
        if (other==null||getClass()!=other.getClass()) {
            return false;
        }
        Rect rectangle=(Rect)other;
        return bits(minX)==bits(rectangle.minX)&&bits(minY)==bits(rectangle.minY)&&bits(maxX)==bits(rectangle.maxX)&&bits(maxY)==bits(rectangle.maxY);
    }
    @Override
    public int hashCode() {
        int result=17;
        result=31*result+longBits(minX);
        result=31*result+longBits(minY);
        result=31*result+longBits(maxX);
        result=31*result+longBits(maxY);
        return result;
    }
    @Override
    public String toString() {
        return "Rect{minX="+minX+", minY="+minY+", maxX="+maxX+", maxY="+maxY+"}";
    }
    private static long bits(double value) {
        return Double.doubleToLongBits(value);
    }
    private static int longBits(double value) {
        long bits=Double.doubleToLongBits(value);
        return (int)(bits^(bits>>>32));
    }
}
