package com.vectorium.core.raster;
import java.util.ArrayList;
import java.util.List;
import com.vectorium.core.model.Color;
import com.vectorium.core.model.PathId;
import com.vectorium.core.model.VectorPath;
public final class RasterVectorizer {
    private static final Color BLACK=new Color(0, 0, 0, 255);
    private RasterVectorizer() {
    }
    public static List<VectorPath> vectorize(RasterFrame frame) {
        if (frame==null) {
            throw new IllegalArgumentException("frame must not be null");
        }
        return vectorize(frame.getWidth(), frame.getHeight(), frame.getOwnedPixels());
    }
    public static List<VectorPath> vectorize(int width, int height, int[] rgba) {
        int pixelLength=pixelLength(width, height);
        if (rgba==null) {
            throw new IllegalArgumentException("rgba must not be null");
        }
        if (rgba.length!=pixelLength) {
            throw new IllegalArgumentException("rgba length must equal width multiplied by height");
        }
        List<VectorPath> paths=new ArrayList<VectorPath>();
        int maximumRuns=width/2+width%2;
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
            int runCount=collectRuns(rgba, width, y, runStarts, runEnds);
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
                    appendRectangle(paths, activeRunTops[activeIndex], activeRunStarts[activeIndex], activeRunEnds[activeIndex]+1, y);
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
            appendRectangle(paths, activeRunTops[activeIndex], activeRunStarts[activeIndex], activeRunEnds[activeIndex]+1, height);
        }
        return paths;
    }
    private static int collectRuns(int[] rgba, int width, int y, int[] runStarts, int[] runEnds) {
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
            runStarts[runCount]=start;
            runEnds[runCount]=x-1;
            runCount++;
        }
        return runCount;
    }
    private static boolean isForeground(int pixel) {
        return (pixel&0x00FFFFFF)==0;
    }
    private static void appendRectangle(List<VectorPath> paths, int top, int left, int right, int bottom) {
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
