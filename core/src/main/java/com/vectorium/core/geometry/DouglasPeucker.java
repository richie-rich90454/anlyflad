package com.vectorium.core.geometry;
public final class DouglasPeucker {
    private DouglasPeucker() {
    }
    public static int simplify(double[] xy, boolean closed, double tolerance, boolean[] keep, int[] stack, int[] outputIndices) {
        if (xy==null) {
            throw new IllegalArgumentException("xy must not be null");
        }
        if (xy.length%2!=0) {
            throw new IllegalArgumentException("xy must contain coordinate pairs");
        }
        int pointCount=xy.length/2;
        if (pointCount>Integer.MAX_VALUE/2) {
            throw new IllegalArgumentException("point count is too large");
        }
        if (!Double.isFinite(tolerance)||tolerance<0.0) {
            throw new IllegalArgumentException("tolerance must be finite and nonnegative");
        }
        if (keep==null||keep.length!=pointCount) {
            throw new IllegalArgumentException("keep must have one entry per point");
        }
        if (stack==null||stack.length!=pointCount*2) {
            throw new IllegalArgumentException("stack must have two entries per point");
        }
        if (outputIndices==null||outputIndices.length!=pointCount) {
            throw new IllegalArgumentException("outputIndices must have one entry per point");
        }
        for (int index=0;index<xy.length;index++) {
            if (!Double.isFinite(xy[index])) {
                throw new IllegalArgumentException("xy must be finite");
            }
        }
        for (int index=0;index<pointCount;index++) {
            keep[index]=false;
        }
        double toleranceSquared=tolerance*tolerance;
        if (pointCount==0) {
            return 0;
        }
        if (!closed) {
            keep[0]=true;
            keep[pointCount-1]=true;
            simplifyRange(xy, pointCount, 0, pointCount-1, false, toleranceSquared, keep, stack);
        } else {
            int minimumIndex=lexicographicMinimum(xy, pointCount);
            int maximumIndex=lexicographicMaximum(xy, pointCount);
            keep[minimumIndex]=true;
            if (minimumIndex==maximumIndex) {
                outputIndices[0]=minimumIndex;
                return 1;
            }
            keep[maximumIndex]=true;
            simplifyRange(xy, pointCount, minimumIndex, maximumIndex, true, toleranceSquared, keep, stack);
            simplifyRange(xy, pointCount, maximumIndex, minimumIndex, true, toleranceSquared, keep, stack);
        }
        int outputCount=0;
        for (int index=0;index<pointCount;index++) {
            if (keep[index]) {
                outputIndices[outputCount]=index;
                outputCount++;
            }
        }
        return outputCount;
    }
    private static void simplifyRange(double[] xy, int pointCount, int first, int last, boolean cyclic, double toleranceSquared, boolean[] keep, int[] stack) {
        if (first==last) {
            return;
        }
        int span=cyclic?(last-first+pointCount)%pointCount:last-first;
        if (span<=1) {
            return;
        }
        int stackSize=0;
        stack[stackSize]=first;
        stack[stackSize+1]=last;
        stackSize+=2;
        while (stackSize>0) {
            stackSize-=2;
            int rangeStart=stack[stackSize];
            int rangeEnd=stack[stackSize+1];
            int rangeSpan=cyclic?(rangeEnd-rangeStart+pointCount)%pointCount:rangeEnd-rangeStart;
            if (rangeSpan<=1) {
                continue;
            }
            double startX=xy[rangeStart*2];
            double startY=xy[rangeStart*2+1];
            double endX=xy[rangeEnd*2];
            double endY=xy[rangeEnd*2+1];
            int farthest=-1;
            double farthestDistance=0.0;
            for (int offset=1;offset<rangeSpan;offset++) {
                int index=rangeStart+offset;
                if (cyclic&&index>=pointCount) {
                    index-=pointCount;
                }
                double distance=pointSegmentDistanceSquared(xy[index*2], xy[index*2+1], startX, startY, endX, endY);
                if (farthest<0||distance>farthestDistance) {
                    farthest=index;
                    farthestDistance=distance;
                }
            }
            if (farthest>=0&&farthestDistance>toleranceSquared) {
                keep[farthest]=true;
                if (stackSize+4>stack.length) {
                    throw new IllegalStateException("simplification stack capacity exceeded");
                }
                stack[stackSize]=rangeStart;
                stack[stackSize+1]=farthest;
                stack[stackSize+2]=farthest;
                stack[stackSize+3]=rangeEnd;
                stackSize+=4;
            }
        }
    }
    private static int lexicographicMinimum(double[] xy, int pointCount) {
        int result=0;
        double resultX=xy[0];
        double resultY=xy[1];
        for (int index=1;index<pointCount;index++) {
            double x=xy[index*2];
            double y=xy[index*2+1];
            if (x<resultX||(x==resultX&&y<resultY)) {
                result=index;
                resultX=x;
                resultY=y;
            }
        }
        return result;
    }
    private static int lexicographicMaximum(double[] xy, int pointCount) {
        int result=0;
        double resultX=xy[0];
        double resultY=xy[1];
        for (int index=1;index<pointCount;index++) {
            double x=xy[index*2];
            double y=xy[index*2+1];
            if (x>resultX||(x==resultX&&y>resultY)) {
                result=index;
                resultX=x;
                resultY=y;
            }
        }
        return result;
    }
    private static double pointSegmentDistanceSquared(double pointX, double pointY, double startX, double startY, double endX, double endY) {
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
    private static void validateResult(double value) {
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException("simplification distance must be finite");
        }
    }
}
