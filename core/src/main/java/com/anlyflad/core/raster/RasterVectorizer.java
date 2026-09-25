package com.anlyflad.core.raster;
import java.util.ArrayList;
import java.util.List;
import com.anlyflad.core.model.Color;
import com.anlyflad.core.model.PathId;
import com.anlyflad.core.model.VectorPath;
public final class RasterVectorizer {
    public static final int DEFAULT_MAX_PATHS=1000000;
    private static final Color BLACK=new Color(0, 0, 0, 255);
    private RasterVectorizer() {
    }
    public static List<VectorPath> vectorize(RasterFrame frame) {
        return vectorize(frame, DEFAULT_MAX_PATHS);
    }
    public static List<VectorPath> vectorize(RasterFrame frame, int maxPaths) {
        if (frame==null) {
            throw new IllegalArgumentException("frame must not be null");
        }
        return vectorize(frame.getWidth(), frame.getHeight(), frame.getOwnedPixels(), maxPaths);
    }
    public static List<VectorPath> vectorize(int width, int height, int[] rgba) {
        return vectorize(width, height, rgba, DEFAULT_MAX_PATHS);
    }
    public static List<VectorPath> vectorize(int width, int height, int[] rgba, int maxPaths) {
        int pixelLength=pixelLength(width, height);
        if (rgba==null) {
            throw new IllegalArgumentException("rgba must not be null");
        }
        if (rgba.length!=pixelLength) {
            throw new IllegalArgumentException("rgba length must equal width multiplied by height");
        }
        if (maxPaths<=0||maxPaths>DEFAULT_MAX_PATHS) {
            throw new IllegalArgumentException("maxPaths must be between 1 and "+DEFAULT_MAX_PATHS);
        }
        List<VectorPath> paths=new ArrayList<VectorPath>(Math.min(maxPaths, 1024));
        int maximumRuns=Math.min(width/2+width%2, maxPaths);
        int[] activeRunStarts=new int[maximumRuns];
        int[] activeRunEnds=new int[maximumRuns];
        int[] activeRunTops=new int[maximumRuns];
        int[] runStarts=new int[maximumRuns];
        int[] runEnds=new int[maximumRuns];
        int[] mergedRunStarts=new int[maximumRuns];
        int[] mergedRunEnds=new int[maximumRuns];
        int[] mergedRunTops=new int[maximumRuns];
        int activeRunCount=0;
        for (int y=0;y<height;y++) {
            int runCount=collectRuns(rgba, width, y, runStarts, runEnds, maximumRuns);
            int activeIndex=0;
            int runIndex=0;
            int mergedRunCount=0;
            while (activeIndex<activeRunCount||runIndex<runCount) {
                if (activeIndex<activeRunCount&&runIndex<runCount&&activeRunStarts[activeIndex]==runStarts[runIndex]&&activeRunEnds[activeIndex]==runEnds[runIndex]) {
                    mergedRunStarts[mergedRunCount]=activeRunStarts[activeIndex];
                    mergedRunEnds[mergedRunCount]=activeRunEnds[activeIndex];
                    mergedRunTops[mergedRunCount]=activeRunTops[activeIndex];
                    mergedRunCount++;
                    activeIndex++;
                    runIndex++;
                } else if (runIndex<runCount&&(activeIndex==activeRunCount||runStarts[runIndex]<activeRunStarts[activeIndex])) {
                    mergedRunStarts[mergedRunCount]=runStarts[runIndex];
                    mergedRunEnds[mergedRunCount]=runEnds[runIndex];
                    mergedRunTops[mergedRunCount]=y;
                    mergedRunCount++;
                    runIndex++;
                } else {
                    appendRectangle(paths, activeRunTops[activeIndex], activeRunStarts[activeIndex], activeRunEnds[activeIndex]+1, y, maxPaths);
                    activeIndex++;
                }
            }
            int[] previousStarts=activeRunStarts;
            int[] previousEnds=activeRunEnds;
            int[] previousTops=activeRunTops;
            activeRunStarts=mergedRunStarts;
            activeRunEnds=mergedRunEnds;
            activeRunTops=mergedRunTops;
            mergedRunStarts=previousStarts;
            mergedRunEnds=previousEnds;
            mergedRunTops=previousTops;
            activeRunCount=mergedRunCount;
        }
        for (int activeIndex=0;activeIndex<activeRunCount;activeIndex++) {
            appendRectangle(paths, activeRunTops[activeIndex], activeRunStarts[activeIndex], activeRunEnds[activeIndex]+1, height, maxPaths);
        }
        return paths;
    }
    private static int collectRuns(int[] rgba, int width, int y, int[] runStarts, int[] runEnds, int capacity) {
        int runCount=0;
        int rowOffset=y*width;
        int x=0;
        while (x<width) {
            if (!isForeground(rgba[rowOffset+x])) {
                x++;
                continue;
            }
            int start=x;
            x++;
            while (x<width&&isForeground(rgba[rowOffset+x])) {
                x++;
            }
            if (runCount==capacity) {
                throw new IllegalArgumentException("binary raster output exceeds the path limit of "+capacity);
            }
            runStarts[runCount]=start;
            runEnds[runCount]=x-1;
            runCount++;
        }
        return runCount;
    }
    private static boolean isForeground(int pixel) {
        return (pixel>>>24)!=0&&(pixel&0x00FFFFFF)==0;
    }
    private static void appendRectangle(List<VectorPath> paths, int top, int left, int right, int bottom, int maxPaths) {
        if (paths.size()>=maxPaths) {
            throw new IllegalArgumentException("binary raster output exceeds the path limit of "+maxPaths);
        }
        double[] coordinates={left, top, right, top, right, bottom, left, bottom};
        paths.add(new VectorPath(PathId.of(paths.size()), coordinates, true, BLACK, 1.0));
    }
    private static int pixelLength(int width, int height) {
        if (width<=0) {
            throw new IllegalArgumentException("width must be positive");
        }
        if (height<=0) {
            throw new IllegalArgumentException("height must be positive");
        }
        long length=(long)width*(long)height;
        if (length>Integer.MAX_VALUE) {
            throw new IllegalArgumentException("pixel buffer is too large");
        }
        return (int)length;
    }
}
