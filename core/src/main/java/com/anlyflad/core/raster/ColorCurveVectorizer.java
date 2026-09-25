package com.anlyflad.core.raster;

import com.anlyflad.core.geometry.CubicBezierFitter;
import com.anlyflad.core.geometry.DouglasPeucker;
import com.anlyflad.core.model.VectorPath;
import java.util.ArrayList;
import java.util.List;

public final class ColorCurveVectorizer {
    public static final double DEFAULT_TOLERANCE=CubicBezierFitter.DEFAULT_TOLERANCE;
    public static final double DEFAULT_CURVE_TOLERANCE=DEFAULT_TOLERANCE;
    public static final int DEFAULT_MAX_PATHS=ColorContourVectorizer.DEFAULT_MAX_PATHS;
    public static final int DEFAULT_MAX_VERTICES=ColorContourVectorizer.DEFAULT_MAX_VERTICES;
    public static final int MAX_PATHS=ColorContourVectorizer.MAX_PATHS;
    public static final int MAX_VERTICES=ColorContourVectorizer.MAX_VERTICES;
    private static final double MIN_SIMPLIFICATION_TOLERANCE=2.0;
    private static final double MAX_SIMPLIFICATION_TOLERANCE=8.0;
    private static final double CORNER_CUT_RADIUS=2.0;
    private static final int MAX_SMOOTHED_VERTICES=16384;
    private static final int MAX_FIT_VERTICES=16384;
    private static final double MIN_CORNER_RADIUS=0.5;
    private static final double MAX_CORNER_RADIUS=2.0;
    private static final double KAPPA=0.5522847498307936;
    private static final double MIN_DISTANCE=1.0e-9;
    private ColorCurveVectorizer() {
    }

    public static List<VectorPath> vectorize(RasterFrame frame) {
        return vectorize(frame, DEFAULT_TOLERANCE, DEFAULT_MAX_PATHS, DEFAULT_MAX_VERTICES);
    }

    public static List<VectorPath> vectorize(RasterFrame frame, double tolerance) {
        return vectorize(frame, tolerance, DEFAULT_MAX_PATHS, DEFAULT_MAX_VERTICES);
    }

    public static List<VectorPath> vectorize(RasterFrame frame, int maxPaths, int maxVertices) {
        return vectorize(frame, DEFAULT_TOLERANCE, maxPaths, maxVertices);
    }

    public static List<VectorPath> vectorize(RasterFrame frame, int maxPaths, int maxVertices, double tolerance) {
        return vectorize(frame, tolerance, maxPaths, maxVertices);
    }

    public static List<VectorPath> vectorize(RasterFrame frame, double tolerance, int maxPaths, int maxVertices) {
        validateTolerance(tolerance);
        if (frame==null) {
            throw new IllegalArgumentException("frame must not be null");
        }
        return fitPaths(ColorContourVectorizer.vectorize(frame, maxPaths, maxVertices), tolerance);
    }

    public static List<VectorPath> vectorize(int width, int height, int[] argb) {
        return vectorize(width, height, argb, DEFAULT_TOLERANCE, DEFAULT_MAX_PATHS, DEFAULT_MAX_VERTICES);
    }

    public static List<VectorPath> vectorize(int width, int height, int[] argb, double tolerance) {
        return vectorize(width, height, argb, tolerance, DEFAULT_MAX_PATHS, DEFAULT_MAX_VERTICES);
    }

    public static List<VectorPath> vectorize(int width, int height, int[] argb, int maxPaths, int maxVertices) {
        return vectorize(width, height, argb, DEFAULT_TOLERANCE, maxPaths, maxVertices);
    }

    public static List<VectorPath> vectorize(int width, int height, int[] argb, int maxPaths, int maxVertices, double tolerance) {
        return vectorize(width, height, argb, tolerance, maxPaths, maxVertices);
    }

    public static List<VectorPath> vectorize(int width, int height, int[] argb, double tolerance, int maxPaths, int maxVertices) {
        validateTolerance(tolerance);
        return fitPaths(ColorContourVectorizer.vectorize(width, height, argb, maxPaths, maxVertices), tolerance);
    }

    public static List<VectorPath> vectorizeForeground(RasterFrame frame) {
        return vectorizeForeground(frame, DEFAULT_TOLERANCE, DEFAULT_MAX_PATHS, DEFAULT_MAX_VERTICES);
    }

    public static List<VectorPath> vectorizeForeground(RasterFrame frame, double tolerance) {
        return vectorizeForeground(frame, tolerance, DEFAULT_MAX_PATHS, DEFAULT_MAX_VERTICES);
    }

    public static List<VectorPath> vectorizeForeground(RasterFrame frame, int maxPaths, int maxVertices) {
        return vectorizeForeground(frame, DEFAULT_TOLERANCE, maxPaths, maxVertices);
    }

    public static List<VectorPath> vectorizeForeground(RasterFrame frame, int maxPaths, int maxVertices, double tolerance) {
        return vectorizeForeground(frame, tolerance, maxPaths, maxVertices);
    }

    public static List<VectorPath> vectorizeForeground(RasterFrame frame, double tolerance, int maxPaths, int maxVertices) {
        validateTolerance(tolerance);
        if (frame==null) {
            throw new IllegalArgumentException("frame must not be null");
        }
        return fitPaths(ColorContourVectorizer.vectorizeForeground(frame, maxPaths, maxVertices), tolerance);
    }

    public static List<VectorPath> vectorizeForeground(int width, int height, int[] argb) {
        return vectorizeForeground(width, height, argb, DEFAULT_TOLERANCE, DEFAULT_MAX_PATHS, DEFAULT_MAX_VERTICES);
    }

    public static List<VectorPath> vectorizeForeground(int width, int height, int[] argb, double tolerance) {
        return vectorizeForeground(width, height, argb, tolerance, DEFAULT_MAX_PATHS, DEFAULT_MAX_VERTICES);
    }

    public static List<VectorPath> vectorizeForeground(int width, int height, int[] argb, int maxPaths, int maxVertices) {
        return vectorizeForeground(width, height, argb, DEFAULT_TOLERANCE, maxPaths, maxVertices);
    }

    public static List<VectorPath> vectorizeForeground(int width, int height, int[] argb, int maxPaths, int maxVertices, double tolerance) {
        return vectorizeForeground(width, height, argb, tolerance, maxPaths, maxVertices);
    }

    public static List<VectorPath> vectorizeForeground(int width, int height, int[] argb, double tolerance, int maxPaths, int maxVertices) {
        validateTolerance(tolerance);
        return fitPaths(ColorContourVectorizer.vectorizeForeground(width, height, argb, maxPaths, maxVertices), tolerance);
    }

    private static List<VectorPath> fitPaths(List<VectorPath> paths, double tolerance) {
        List<VectorPath> fitted=new ArrayList<VectorPath>(paths.size());
        double simplificationTolerance=rasterSimplificationTolerance(tolerance);
        for (int pathIndex=0;pathIndex<paths.size();pathIndex++) {
            VectorPath path=paths.get(pathIndex);
            double[][] sourceRings=path.getRingCoordinates();
            List<double[]> fallbackRings=new ArrayList<double[]>(sourceRings.length);
            double[][] cubic=new double[sourceRings.length][];
            for (int ringIndex=0;ringIndex<sourceRings.length;ringIndex++) {
                FittedRing fittedRing=fitRasterRing(sourceRings[ringIndex],tolerance,simplificationTolerance);
                fallbackRings.add(fittedRing.fallback);
                cubic[ringIndex]=fittedRing.cubic;
            }
            fitted.add(new VectorPath(path.getId(),fallbackRings,cubic,path.getFill(),path.getOpacity(),path.getFillRule(),path.getArea()));
        }
        return fitted;
    }

    private static FittedRing fitRasterRing(double[] source,double tolerance,double simplificationTolerance) {
        double[] ring=cleanRing(source);
        if (ring.length<6) {
            return new FittedRing(ring,flatten(lineSegments(ring)));
        }
        double[] simplified=simplifyRing(ring,simplificationTolerance);
        double[] polygon=cornerCut(simplified,CORNER_CUT_RADIUS);
        polygon=cleanRing(polygon);
        if (polygon.length<6) {
            return new FittedRing(ring,flatten(lineSegments(ring)));
        }
        if (polygon.length/2<=MAX_FIT_VERTICES) {
            List<double[]> segments=new CubicBezierFitter(tolerance).fit(polygon);
            double[] reduced=segmentEndpoints(segments);
            reduced=cleanRing(reduced);
            if (reduced.length>=6) {
                return new FittedRing(reduced,splineSegments(reduced));
            }
            return new FittedRing(polygon,flatten(segments));
        }
        return new FittedRing(polygon,splineSegments(polygon));
    }

    private static double rasterSimplificationTolerance(double tolerance) {
        if (tolerance==0.0) {
            return 0.0;
        }
        return Math.min(MAX_SIMPLIFICATION_TOLERANCE,Math.max(MIN_SIMPLIFICATION_TOLERANCE,tolerance*1.5));
    }

    private static double[] cleanRing(double[] source) {
        int pointCount=source.length/2;
        if (pointCount>1&&samePoint(source[0],source[1],source[(pointCount-1)*2],source[(pointCount-1)*2+1])) {
            pointCount--;
        }
        int write=0;
        for (int index=0;index<pointCount;index++) {
            double x=source[index*2];
            double y=source[index*2+1];
            if (write==0||!samePoint(x,y,source[(write-1)*2],source[(write-1)*2+1])) {
                source[write*2]=x;
                source[write*2+1]=y;
                write++;
            }
        }
        if (write>1&&samePoint(source[0],source[1],source[(write-1)*2],source[(write-1)*2+1])) {
            write--;
        }
        double[] result=new double[write*2];
        System.arraycopy(source,0,result,0,write*2);
        return result;
    }

    private static double[] simplifyRing(double[] ring,double tolerance) {
        int pointCount=ring.length/2;
        if (tolerance>0.0&&pointCount>MAX_SMOOTHED_VERTICES) {
            ring=limitVertices(ring,MAX_SMOOTHED_VERTICES);
            pointCount=ring.length/2;
        }
        if (tolerance==0.0||pointCount<=8) {
            return removeCollinear(ring);
        }
        boolean[] keep=new boolean[pointCount];
        int[] stack=new int[pointCount*2];
        int[] outputIndices=new int[pointCount];
        int count=DouglasPeucker.simplify(ring,true,tolerance,keep,stack,outputIndices);
        if (count<4) {
            return removeCollinear(ring);
        }
        double[] result=new double[pointCount*2];
        int write=0;
        if (!keep[0]) {
            result[write++]=ring[0];
            result[write++]=ring[1];
        }
        for (int index=0;index<count;index++) {
            int sourceIndex=outputIndices[index];
            if (sourceIndex==0&&!keep[0]) {
                continue;
            }
            double x=ring[sourceIndex*2];
            double y=ring[sourceIndex*2+1];
            if (write==0||!samePoint(x,y,result[write-2],result[write-1])) {
                result[write++]=x;
                result[write++]=y;
            }
        }
        if (write<6) {
            return removeCollinear(ring);
        }
        double[] simplified=new double[write];
        System.arraycopy(result,0,simplified,0,write);
        return cleanRing(simplified);
    }

    private static double[] limitVertices(double[] ring,int maximum) {
        int pointCount=ring.length/2;
        if (pointCount<=maximum) {
            return ring;
        }
        int step=Math.max(1,(pointCount-2)/(maximum-2));
        double[] result=new double[maximum*2];
        int write=0;
        result[write++]=ring[0];
        result[write++]=ring[1];
        int sourceIndex=1;
        while (sourceIndex<pointCount-1&&write<(maximum-1)*2) {
            result[write++]=ring[sourceIndex*2];
            result[write++]=ring[sourceIndex*2+1];
            sourceIndex+=step;
        }
        result[write++]=ring[(pointCount-1)*2];
        result[write++]=ring[(pointCount-1)*2+1];
        double[] limited=new double[write];
        System.arraycopy(result,0,limited,0,write);
        return cleanRing(limited);
    }

    private static double[] removeCollinear(double[] ring) {
        int pointCount=ring.length/2;
        if (pointCount<=3) {
            return ring;
        }
        boolean[] remove=new boolean[pointCount];
        for (int pass=0;pass<3;pass++) {
            boolean removed=false;
            for (int index=0;index<pointCount;index++) {
                int previous=(index+pointCount-1)%pointCount;
                int next=(index+1)%pointCount;
                if (collinear(ring,previous,index,next)) {
                    remove[index]=true;
                    removed=true;
                }
            }
            if (!removed) {
                break;
            }
            int write=0;
            for (int index=0;index<pointCount;index++) {
                if (!remove[index]) {
                    ring[write*2]=ring[index*2];
                    ring[write*2+1]=ring[index*2+1];
                    write++;
                }
            }
            if (write==pointCount) {
                break;
            }
            pointCount=write;
            if (pointCount<=3) {
                break;
            }
            java.util.Arrays.fill(remove,0,pointCount,false);
        }
        double[] result=new double[pointCount*2];
        System.arraycopy(ring,0,result,0,pointCount*2);
        return cleanRing(result);
    }

    private static boolean collinear(double[] ring,int first,int middle,int last) {
        double ax=ring[first*2];
        double ay=ring[first*2+1];
        double bx=ring[middle*2];
        double by=ring[middle*2+1];
        double cx=ring[last*2];
        double cy=ring[last*2+1];
        double abx=bx-ax;
        double aby=by-ay;
        double bcx=cx-bx;
        double bcy=cy-by;
        double cross=abx*bcy-aby*bcx;
        double dot=abx*bcx+aby*bcy;
        return Math.abs(cross)<=MIN_DISTANCE&&dot>=0.0;
    }

    private static double[] cornerCut(double[] ring,double radius) {
        int count=ring.length/2;
        if (count<3) {
            return ring;
        }
        double[] result=new double[count*2];
        for (int index=0;index<count;index++) {
            int next=(index+1)%count;
            double x=ring[index*2];
            double y=ring[index*2+1];
            double dx=ring[next*2]-x;
            double dy=ring[next*2+1]-y;
            double length=Math.hypot(dx,dy);
            double localRadius=Math.min(radius,length*0.5);
            if (length>MIN_DISTANCE) {
                result[index*2]=x+dx/length*localRadius;
                result[index*2+1]=y+dy/length*localRadius;
            } else {
                result[index*2]=x;
                result[index*2+1]=y;
            }
        }
        return result;
    }

    private static double[] segmentEndpoints(List<double[]> segments) {
        double[] result=new double[segments.size()*2];
        for (int index=0;index<segments.size();index++) {
            result[index*2]=segments.get(index)[6];
            result[index*2+1]=segments.get(index)[7];
        }
        return result;
    }

    private static double[] splineSegments(double[] ring) {
        int count=ring.length/2;
        double[] result=new double[count*8];
        for (int index=0;index<count;index++) {
            int previous=(index+count-1)%count;
            int current=index;
            int next=(index+1)%count;
            int after=(index+2)%count;
            double currentX=ring[current*2];
            double currentY=ring[current*2+1];
            double nextX=ring[next*2];
            double nextY=ring[next*2+1];
            double control1X=currentX+(nextX-ring[previous*2])/6.0;
            double control1Y=currentY+(nextY-ring[previous*2+1])/6.0;
            double control2X=nextX-(ring[after*2]-currentX)/6.0;
            double control2Y=nextY-(ring[after*2+1]-currentY)/6.0;
            int offset=index*8;
            result[offset]=currentX;
            result[offset+1]=currentY;
            result[offset+2]=control1X;
            result[offset+3]=control1Y;
            result[offset+4]=control2X;
            result[offset+5]=control2Y;
            result[offset+6]=nextX;
            result[offset+7]=nextY;
        }
        return result;
    }

    private static FittedRing roundCorners(double[] polygon,double tolerance) {
        int count=polygon.length/2;
        if (count<3) {
            return new FittedRing(polygon,flatten(lineSegments(polygon)));
        }
        double[] incomingX=new double[count];
        double[] incomingY=new double[count];
        double[] outgoingX=new double[count];
        double[] outgoingY=new double[count];
        double[] incomingLength=new double[count];
        double[] outgoingLength=new double[count];
        double[] radius=new double[count];
        double[] startX=new double[count];
        double[] startY=new double[count];
        double[] endX=new double[count];
        double[] endY=new double[count];
        double baseRadius=tolerance<=0.0?0.0:Math.min(MAX_CORNER_RADIUS,MIN_CORNER_RADIUS+tolerance);
        boolean curved=false;
        for (int index=0;index<count;index++) {
            int previous=(index+count-1)%count;
            int next=(index+1)%count;
            double currentX=polygon[index*2];
            double currentY=polygon[index*2+1];
            incomingX[index]=currentX-polygon[previous*2];
            incomingY[index]=currentY-polygon[previous*2+1];
            outgoingX[index]=polygon[next*2]-currentX;
            outgoingY[index]=polygon[next*2+1]-currentY;
            incomingLength[index]=Math.hypot(incomingX[index],incomingY[index]);
            outgoingLength[index]=Math.hypot(outgoingX[index],outgoingY[index]);
            if (incomingLength[index]<=MIN_DISTANCE||outgoingLength[index]<=MIN_DISTANCE) {
                radius[index]=0.0;
            } else {
                double cross=incomingX[index]*outgoingY[index]-incomingY[index]*outgoingX[index];
                if (Math.abs(cross)<=MIN_DISTANCE) {
                    radius[index]=0.0;
                } else {
                    radius[index]=Math.min(baseRadius,Math.min(incomingLength[index],outgoingLength[index])*0.5);
                    curved|=radius[index]>MIN_DISTANCE;
                }
            }
            startX[index]=currentX-(incomingLength[index]>MIN_DISTANCE?incomingX[index]/incomingLength[index]*radius[index]:0.0);
            startY[index]=currentY-(incomingLength[index]>MIN_DISTANCE?incomingY[index]/incomingLength[index]*radius[index]:0.0);
            endX[index]=currentX+(outgoingLength[index]>MIN_DISTANCE?outgoingX[index]/outgoingLength[index]*radius[index]:0.0);
            endY[index]=currentY+(outgoingLength[index]>MIN_DISTANCE?outgoingY[index]/outgoingLength[index]*radius[index]:0.0);
        }
        if (!curved) {
            return new FittedRing(polygon,flatten(lineSegments(polygon)));
        }
        double[] fallback=new double[count*2];
        for (int index=0;index<count;index++) {
            int source=(index+count-1)%count;
            fallback[index*2]=endX[source];
            fallback[index*2+1]=endY[source];
        }
        List<double[]> segments=new ArrayList<double[]>(count);
        for (int index=0;index<count;index++) {
            int previous=(index+count-1)%count;
            double startPointX=endX[previous];
            double startPointY=endY[previous];
            double endPointX=endX[index];
            double endPointY=endY[index];
            double leadX=startX[index]-startPointX;
            double leadY=startY[index]-startPointY;
            double control1X;
            double control1Y;
            if (leadX*leadX+leadY*leadY>MIN_DISTANCE*MIN_DISTANCE) {
                control1X=startPointX+leadX/3.0;
                control1Y=startPointY+leadY/3.0;
            } else {
                double scale=radius[index]>MIN_DISTANCE?KAPPA*radius[index]/3.0:0.0;
                control1X=startPointX+(incomingLength[index]>MIN_DISTANCE?incomingX[index]/incomingLength[index]*scale:0.0);
                control1Y=startPointY+(incomingLength[index]>MIN_DISTANCE?incomingY[index]/incomingLength[index]*scale:0.0);
            }
            double outgoingScale=radius[index]>MIN_DISTANCE?KAPPA*radius[index]:0.0;
            double control2X=endPointX-(outgoingLength[index]>MIN_DISTANCE?outgoingX[index]/outgoingLength[index]*outgoingScale:0.0);
            double control2Y=endPointY-(outgoingLength[index]>MIN_DISTANCE?outgoingY[index]/outgoingLength[index]*outgoingScale:0.0);
            if (!Double.isFinite(control1X)||!Double.isFinite(control1Y)||!Double.isFinite(control2X)||!Double.isFinite(control2Y)) {
                return new FittedRing(polygon,flatten(lineSegments(polygon)));
            }
            segments.add(new double[]{startPointX,startPointY,control1X,control1Y,control2X,control2Y,endPointX,endPointY});
        }
        return new FittedRing(fallback,flatten(segments));
    }

    private static List<double[]> lineSegments(double[] ring) {
        int count=ring.length/2;
        List<double[]> segments=new ArrayList<double[]>(count);
        for (int index=0;index<count;index++) {
            int next=(index+1)%count;
            double startX=ring[index*2];
            double startY=ring[index*2+1];
            double endX=ring[next*2];
            double endY=ring[next*2+1];
            segments.add(new double[]{startX,startY,startX+(endX-startX)/3.0,startY+(endY-startY)/3.0,startX+2.0*(endX-startX)/3.0,startY+2.0*(endY-startY)/3.0,endX,endY});
        }
        return segments;
    }

    private static double[] flatten(List<double[]> segments) {
        double[] result=new double[segments.size()*8];
        for (int index=0;index<segments.size();index++) {
            System.arraycopy(segments.get(index),0,result,index*8,8);
        }
        return result;
    }

    private static boolean samePoint(double firstX,double firstY,double secondX,double secondY) {
        return firstX==secondX&&firstY==secondY;
    }

    private static final class FittedRing {
        private final double[] fallback;
        private final double[] cubic;
        private FittedRing(double[] fallback,double[] cubic) {
            this.fallback=fallback;
            this.cubic=cubic;
        }
    }

    private static void validateTolerance(double tolerance) {
        if (!Double.isFinite(tolerance)||tolerance<0.0) {
            throw new IllegalArgumentException("tolerance must be finite and nonnegative");
        }
    }
}
