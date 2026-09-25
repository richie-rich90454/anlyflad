package com.vectorium.core.raster;

import com.vectorium.core.geometry.CubicBezierFitter;
import com.vectorium.core.model.VectorPath;
import java.util.ArrayList;
import java.util.List;

public final class ColorCurveVectorizer {
    public static final double DEFAULT_TOLERANCE=CubicBezierFitter.DEFAULT_TOLERANCE;
    public static final double DEFAULT_CURVE_TOLERANCE=DEFAULT_TOLERANCE;
    public static final int DEFAULT_MAX_PATHS=ColorContourVectorizer.DEFAULT_MAX_PATHS;
    public static final int DEFAULT_MAX_VERTICES=ColorContourVectorizer.DEFAULT_MAX_VERTICES;
    public static final int MAX_PATHS=ColorContourVectorizer.MAX_PATHS;
    public static final int MAX_VERTICES=ColorContourVectorizer.MAX_VERTICES;
    private static final int MAX_FIT_VERTICES=16384;
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
        CubicBezierFitter fitter=new CubicBezierFitter(tolerance);
        long totalVertices=0L;
        for (int pathIndex=0;pathIndex<paths.size();pathIndex++) {
            double[][] rings=paths.get(pathIndex).getRingCoordinates();
            for (int ringIndex=0;ringIndex<rings.length;ringIndex++) {
                totalVertices+=rings[ringIndex].length/2L;
            }
        }
        boolean useExactLines=totalVertices>MAX_FIT_VERTICES;
        for (int pathIndex=0;pathIndex<paths.size();pathIndex++) {
            VectorPath path=paths.get(pathIndex);
            double[][] fallback=path.getRingCoordinates();
            double[][] cubic=new double[fallback.length][];
            for (int ringIndex=0;ringIndex<fallback.length;ringIndex++) {
                List<double[]> segments=isRectilinear(fallback[ringIndex])&&(useExactLines||fallback[ringIndex].length/2>256)?lineSegments(fallback[ringIndex]):fitter.fit(fallback[ringIndex]);
                cubic[ringIndex]=flatten(segments);
            }
            fitted.add(new VectorPath(path.getId(), path.getRings(), cubic, path.getFill(), path.getOpacity(), path.getFillRule(), path.getArea()));
        }
        return fitted;
    }

    private static boolean isRectilinear(double[] ring) {
        int count=ring.length/2;
        for (int index=0;index<count;index++) {
            int next=(index+1)%count;
            if (ring[index*2]!=ring[next*2]&&ring[index*2+1]!=ring[next*2+1]) {
                return false;
            }
        }
        return true;
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

    private static void validateTolerance(double tolerance) {
        if (!Double.isFinite(tolerance)||tolerance<0.0) {
            throw new IllegalArgumentException("tolerance must be finite and nonnegative");
        }
    }
}
