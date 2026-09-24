package com.vectorium.core.geometry;
public final class BezierFlattener {
    private final int maxSteps;
    private final double[] outputCoordinates;
    private int pointCount;
    public BezierFlattener(int maxSteps) {
        if (maxSteps<=0||maxSteps>Integer.MAX_VALUE/2-1) {
            throw new IllegalArgumentException("maxSteps must be positive and fit the output buffer");
        }
        this.maxSteps=maxSteps;
        this.outputCoordinates=new double[2*(maxSteps+1)];
        this.pointCount=0;
    }
    public int flatten(double x0, double y0, double x1, double y1, double x2, double y2, double x3, double y3, int steps) {
        if (steps<1||steps>maxSteps) {
            throw new IllegalArgumentException("steps must be between one and maxSteps");
        }
        validateFinite(x0);
        validateFinite(y0);
        validateFinite(x1);
        validateFinite(y1);
        validateFinite(x2);
        validateFinite(y2);
        validateFinite(x3);
        validateFinite(y3);
        outputCoordinates[0]=x0;
        outputCoordinates[1]=y0;
        for (int step=1;step<steps;step++) {
            double t=step/(double)steps;
            double inverse=1.0-t;
            double x=inverse*inverse*inverse*x0+3.0*inverse*inverse*t*x1+3.0*inverse*t*t*x2+t*t*t*x3;
            double y=inverse*inverse*inverse*y0+3.0*inverse*inverse*t*y1+3.0*inverse*t*t*y2+t*t*t*y3;
            validateResult(x);
            validateResult(y);
            outputCoordinates[step*2]=x;
            outputCoordinates[step*2+1]=y;
        }
        outputCoordinates[steps*2]=x3;
        outputCoordinates[steps*2+1]=y3;
        pointCount=steps+1;
        return pointCount;
    }
    public int flatten(double x0, double y0, double x1, double y1, double x2, double y2, double x3, double y3) {
        return flatten(x0, y0, x1, y1, x2, y2, x3, y3, maxSteps);
    }
    public int flatten(double[] first, double[] second, double[] third, double[] fourth) {
        validateControlPoint(first);
        validateControlPoint(second);
        validateControlPoint(third);
        validateControlPoint(fourth);
        return flatten(first[0], first[1], second[0], second[1], third[0], third[1], fourth[0], fourth[1]);
    }
    public int flatten(double[] controlPoints) {
        if (controlPoints==null||controlPoints.length!=8) {
            throw new IllegalArgumentException("controlPoints must contain eight coordinates");
        }
        return flatten(controlPoints[0], controlPoints[1], controlPoints[2], controlPoints[3], controlPoints[4], controlPoints[5], controlPoints[6], controlPoints[7]);
    }
    public int getPointCount() {
        return pointCount;
    }
    public double[] getOutputCoordinates() {
        return outputCoordinates;
    }
    private static void validateControlPoint(double[] value) {
        if (value==null||value.length!=2) {
            throw new IllegalArgumentException("control points must contain exactly two coordinates");
        }
        validateFinite(value[0]);
        validateFinite(value[1]);
    }
    private static void validateFinite(double value) {
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException("control coordinates must be finite");
        }
    }
    private static void validateResult(double value) {
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException("flattened coordinates must be finite");
        }
    }
}
