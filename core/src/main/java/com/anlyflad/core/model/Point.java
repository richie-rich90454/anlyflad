package com.anlyflad.core.model;
public final class Point {
    private final double x;
    private final double y;
    public Point(double x, double y) {
        if (!Double.isFinite(x)) {
            throw new IllegalArgumentException("x must be finite");
        }
        if (!Double.isFinite(y)) {
            throw new IllegalArgumentException("y must be finite");
        }
        this.x=x;
        this.y=y;
    }
    public double getX() {
        return x;
    }
    public double getY() {
        return y;
    }
    @Override
    public boolean equals(Object other) {
        if (this==other) {
            return true;
        }
        if (other==null||getClass()!=other.getClass()) {
            return false;
        }
        Point point=(Point)other;
        return Double.doubleToLongBits(x)==Double.doubleToLongBits(point.x)&&Double.doubleToLongBits(y)==Double.doubleToLongBits(point.y);
    }
    @Override
    public int hashCode() {
        int result=17;
        result=31*result+longBits(x);
        result=31*result+longBits(y);
        return result;
    }
    @Override
    public String toString() {
        return "Point{x="+x+", y="+y+"}";
    }
    private static int longBits(double value) {
        long bits=Double.doubleToLongBits(value);
        return (int)(bits^(bits>>>32));
    }
}
