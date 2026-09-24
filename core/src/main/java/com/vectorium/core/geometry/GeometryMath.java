package com.vectorium.core.geometry;
public final class GeometryMath {
    private GeometryMath() {
    }
    public static void add(double[] first, double[] second, double[] output) {
        validateVector(first);
        validateVector(second);
        validateOutput(output);
        double x=first[0]+second[0];
        double y=first[1]+second[1];
        validateResult(x);
        validateResult(y);
        output[0]=x;
        output[1]=y;
    }
    public static void subtract(double[] first, double[] second, double[] output) {
        validateVector(first);
        validateVector(second);
        validateOutput(output);
        double x=first[0]-second[0];
        double y=first[1]-second[1];
        validateResult(x);
        validateResult(y);
        output[0]=x;
        output[1]=y;
    }
    public static void scale(double[] vector, double factor, double[] output) {
        validateVector(vector);
        validateOutput(output);
        validateFinite(factor);
        double x=vector[0]*factor;
        double y=vector[1]*factor;
        validateResult(x);
        validateResult(y);
        output[0]=x;
        output[1]=y;
    }
    public static void lerp(double[] first, double[] second, double amount, double[] output) {
        validateVector(first);
        validateVector(second);
        validateOutput(output);
        validateFinite(amount);
        double x;
        double y;
        if (amount==0.0) {
            x=first[0];
            y=first[1];
        } else if (amount==1.0) {
            x=second[0];
            y=second[1];
        } else {
            x=first[0]+amount*(second[0]-first[0]);
            y=first[1]+amount*(second[1]-first[1]);
        }
        validateResult(x);
        validateResult(y);
        output[0]=x;
        output[1]=y;
    }
    public static double dot(double[] first, double[] second) {
        validateVector(first);
        validateVector(second);
        double firstProduct=first[0]*second[0];
        double secondProduct=first[1]*second[1];
        validateResult(firstProduct);
        validateResult(secondProduct);
        double result=firstProduct+secondProduct;
        validateResult(result);
        return result;
    }
    public static double cross(double[] first, double[] second) {
        validateVector(first);
        validateVector(second);
        double firstProduct=first[0]*second[1];
        double secondProduct=first[1]*second[0];
        validateResult(firstProduct);
        validateResult(secondProduct);
        double result=firstProduct-secondProduct;
        validateResult(result);
        return result;
    }
    public static double distanceSquared(double[] first, double[] second) {
        validateVector(first);
        validateVector(second);
        double dx=first[0]-second[0];
        double dy=first[1]-second[1];
        double result=dx*dx+dy*dy;
        validateResult(result);
        return result;
    }
    public static double distanceSquared(double firstX, double firstY, double secondX, double secondY) {
        validateFinite(firstX);
        validateFinite(firstY);
        validateFinite(secondX);
        validateFinite(secondY);
        double dx=firstX-secondX;
        double dy=firstY-secondY;
        double result=dx*dx+dy*dy;
        validateResult(result);
        return result;
    }
    public static double pointSegmentDistanceSquared(double pointX, double pointY, double startX, double startY, double endX, double endY) {
        validateFinite(pointX);
        validateFinite(pointY);
        validateFinite(startX);
        validateFinite(startY);
        validateFinite(endX);
        validateFinite(endY);
        double dx=endX-startX;
        double dy=endY-startY;
        double lengthSquared=dx*dx+dy*dy;
        validateResult(lengthSquared);
        if (lengthSquared==0.0) {
            double pointDx=pointX-startX;
            double pointDy=pointY-startY;
            double result=pointDx*pointDx+pointDy*pointDy;
            validateResult(result);
            return result;
        }
        double projection=((pointX-startX)*dx+(pointY-startY)*dy)/lengthSquared;
        validateResult(projection);
        if (projection<0.0) {
            projection=0.0;
        } else if (projection>1.0) {
            projection=1.0;
        }
        double closestX=startX+projection*dx;
        double closestY=startY+projection*dy;
        double pointDx=pointX-closestX;
        double pointDy=pointY-closestY;
        double result=pointDx*pointDx+pointDy*pointDy;
        validateResult(result);
        return result;
    }
    public static double shoelace2(double[] coordinates) {
        validatePolygon(coordinates);
        double sum=0.0;
        for (int index=0;index<coordinates.length;index+=2) {
            int nextIndex=(index+2)%coordinates.length;
            double firstProduct=coordinates[index]*coordinates[nextIndex+1];
            double secondProduct=coordinates[nextIndex]*coordinates[index+1];
            validateResult(firstProduct);
            validateResult(secondProduct);
            sum+=firstProduct-secondProduct;
            validateResult(sum);
        }
        return sum;
    }
    public static boolean contains(double[] polygon, double pointX, double pointY) {
        validatePolygon(polygon);
        validateFinite(pointX);
        validateFinite(pointY);
        int pointCount=polygon.length/2;
        if (pointCount<3) {
            return false;
        }
        boolean inside=false;
        int previous=pointCount-1;
        for (int current=0;current<pointCount;current++) {
            int currentOffset=current*2;
            int previousOffset=previous*2;
            double currentX=polygon[currentOffset];
            double currentY=polygon[currentOffset+1];
            double previousX=polygon[previousOffset];
            double previousY=polygon[previousOffset+1];
            if (pointSegmentDistanceSquared(pointX, pointY, previousX, previousY, currentX, currentY)==0.0) {
                return true;
            }
            if ((currentY>pointY)!=(previousY>pointY)) {
                double denominator=previousY-currentY;
                double crossingX=currentX+(pointY-currentY)*(previousX-currentX)/denominator;
                validateResult(crossingX);
                if (pointX<crossingX) {
                    inside=!inside;
                }
            }
            previous=current;
        }
        return inside;
    }
    public static double pointSegmentDistanceSquared(double[] point, double[] segmentStart, double[] segmentEnd) {
        validateVector(point);
        validateVector(segmentStart);
        validateVector(segmentEnd);
        return pointSegmentDistanceSquared(point[0], point[1], segmentStart[0], segmentStart[1], segmentEnd[0], segmentEnd[1]);
    }
    public static boolean contains(double[] polygon, double[] point) {
        validateVector(point);
        return contains(polygon, point[0], point[1]);
    }
    private static void validateOutput(double[] values) {
        if (values==null||values.length!=2) {
            throw new IllegalArgumentException("output must contain exactly two coordinates");
        }
    }
    private static void validateVector(double[] values) {
        if (values==null||values.length!=2) {
            throw new IllegalArgumentException("vectors must contain exactly two coordinates");
        }
        validateFinite(values[0]);
        validateFinite(values[1]);
    }
    private static void validatePolygon(double[] values) {
        if (values==null) {
            throw new IllegalArgumentException("polygon must not be null");
        }
        if (values.length%2!=0) {
            throw new IllegalArgumentException("polygon must contain coordinate pairs");
        }
        for (int index=0;index<values.length;index++) {
            validateFinite(values[index]);
        }
    }
    private static void validateFinite(double value) {
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException("coordinates must be finite");
        }
    }
    private static void validateResult(double value) {
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException("calculation result must be finite");
        }
    }
}
