package com.anlyflad.core.raster;
import java.util.ArrayList;
import java.util.List;
import com.anlyflad.core.model.Color;
import com.anlyflad.core.model.PathId;
import com.anlyflad.core.model.VectorPath;
public final class ColorRasterVectorizer {
    public static final int DEFAULT_MAX_PATHS=Integer.MAX_VALUE;
    public static final int MAX_PATHS=Integer.MAX_VALUE;
    private ColorRasterVectorizer() {
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
    public static List<VectorPath> vectorize(int width, int height, int[] argb, int maxPaths) {
        int pixelLength=pixelLength(width, height);
        if (argb==null) {
            throw new IllegalArgumentException("argb must not be null");
        }
        if (argb.length!=pixelLength) {
            throw new IllegalArgumentException("argb length must equal width multiplied by height");
        }
        if (maxPaths<=0||maxPaths>MAX_PATHS) {
            throw new IllegalArgumentException("maxPaths must be between 1 and "+MAX_PATHS);
        }
        int runCapacity=Math.min(width, maxPaths);
        List<VectorPath> paths=new ArrayList<VectorPath>(Math.min(maxPaths, 1024));
        int[] activeStarts=new int[runCapacity];
        int[] activeEnds=new int[runCapacity];
        int[] activeTops=new int[runCapacity];
        int[] activeColors=new int[runCapacity];
        int[] runStarts=new int[runCapacity];
        int[] runEnds=new int[runCapacity];
        int[] runColors=new int[runCapacity];
        int[] mergedStarts=new int[runCapacity];
        int[] mergedEnds=new int[runCapacity];
        int[] mergedTops=new int[runCapacity];
        int[] mergedColors=new int[runCapacity];
        int activeCount=0;
        for (int y=0;y<height;y++) {
            int runCount=collectRuns(argb, width, y, runStarts, runEnds, runColors, runCapacity);
            int activeIndex=0;
            int runIndex=0;
            int mergedCount=0;
            while (activeIndex<activeCount||runIndex<runCount) {
                if (activeIndex==activeCount) {
                    mergedStarts[mergedCount]=runStarts[runIndex];
                    mergedEnds[mergedCount]=runEnds[runIndex];
                    mergedTops[mergedCount]=y;
                    mergedColors[mergedCount]=runColors[runIndex];
                    mergedCount++;
                    runIndex++;
                    continue;
                }
                if (runIndex==runCount) {
                    appendRectangle(paths, activeTops[activeIndex], activeStarts[activeIndex], activeEnds[activeIndex]+1, y, activeColors[activeIndex], maxPaths);
                    activeIndex++;
                    continue;
                }
                int activeStart=activeStarts[activeIndex];
                int runStart=runStarts[runIndex];
                if (activeStart<runStart) {
                    appendRectangle(paths, activeTops[activeIndex], activeStart, activeEnds[activeIndex]+1, y, activeColors[activeIndex], maxPaths);
                    activeIndex++;
                    continue;
                }
                if (runStart<activeStart) {
                    mergedStarts[mergedCount]=runStart;
                    mergedEnds[mergedCount]=runEnds[runIndex];
                    mergedTops[mergedCount]=y;
                    mergedColors[mergedCount]=runColors[runIndex];
                    mergedCount++;
                    runIndex++;
                    continue;
                }
                int activeEnd=activeEnds[activeIndex];
                int runEnd=runEnds[runIndex];
                if (activeEnd<runEnd) {
                    appendRectangle(paths, activeTops[activeIndex], activeStart, activeEnd+1, y, activeColors[activeIndex], maxPaths);
                    activeIndex++;
                    continue;
                }
                if (runEnd<activeEnd) {
                    mergedStarts[mergedCount]=runStart;
                    mergedEnds[mergedCount]=runEnd;
                    mergedTops[mergedCount]=y;
                    mergedColors[mergedCount]=runColors[runIndex];
                    mergedCount++;
                    runIndex++;
                    continue;
                }
                if (activeColors[activeIndex]==runColors[runIndex]) {
                    mergedStarts[mergedCount]=activeStart;
                    mergedEnds[mergedCount]=activeEnd;
                    mergedTops[mergedCount]=activeTops[activeIndex];
                    mergedColors[mergedCount]=activeColors[activeIndex];
                    mergedCount++;
                    activeIndex++;
                    runIndex++;
                } else {
                    appendRectangle(paths, activeTops[activeIndex], activeStart, activeEnd+1, y, activeColors[activeIndex], maxPaths);
                    mergedStarts[mergedCount]=runStart;
                    mergedEnds[mergedCount]=runEnd;
                    mergedTops[mergedCount]=y;
                    mergedColors[mergedCount]=runColors[runIndex];
                    mergedCount++;
                    activeIndex++;
                    runIndex++;
                }
            }
            int[] previousStarts=activeStarts;
            int[] previousEnds=activeEnds;
            int[] previousTops=activeTops;
            int[] previousColors=activeColors;
            activeStarts=mergedStarts;
            activeEnds=mergedEnds;
            activeTops=mergedTops;
            activeColors=mergedColors;
            mergedStarts=previousStarts;
            mergedEnds=previousEnds;
            mergedTops=previousTops;
            mergedColors=previousColors;
            activeCount=mergedCount;
        }
        for (int activeIndex=0;activeIndex<activeCount;activeIndex++) {
            appendRectangle(paths, activeTops[activeIndex], activeStarts[activeIndex], activeEnds[activeIndex]+1, height, activeColors[activeIndex], maxPaths);
        }
        return paths;
    }
    private static int collectRuns(int[] argb, int width, int y, int[] starts, int[] ends, int[] colors, int capacity) {
        int count=0;
        int offset=y*width;
        int x=0;
        while (x<width) {
            int color=argb[offset+x];
            if ((color>>>24)==0) {
                x++;
                continue;
            }
            int start=x;
            x++;
            while (x<width&&argb[offset+x]==color) {
                x++;
            }
            if (count==capacity) {
                throw new IllegalArgumentException("exact color output exceeds the path limit of "+capacity);
            }
            starts[count]=start;
            ends[count]=x-1;
            colors[count]=color;
            count++;
        }
        return count;
    }
    private static void appendRectangle(List<VectorPath> paths, int top, int left, int right, int bottom, int argb, int maxPaths) {
        if (paths.size()>=maxPaths) {
            throw new IllegalArgumentException("exact color output exceeds the path limit of "+maxPaths+"; increase vectorize.maxPaths or use binary mode");
        }
        double[] coordinates={left, top, right, top, right, bottom, left, bottom};
        paths.add(new VectorPath(PathId.of(paths.size()), coordinates, true, Color.fromArgb(argb), 1.0));
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
