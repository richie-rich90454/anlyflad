package com.anlyflad.core.raster;

import com.anlyflad.core.model.Color;
import com.anlyflad.core.model.PathId;
import com.anlyflad.core.model.VectorPath;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class ColorContourVectorizer {
    public static final int DEFAULT_MAX_PATHS = 250000;
    public static final int MAX_PATHS = 1000000;
    public static final int DEFAULT_MAX_VERTICES = 4000000;
    public static final int MAX_VERTICES = 4000000;

    private static final int MAX_FLOOD_STACK_SPANS = 1048576;
    private static final int UNVISITED = 0;
    private static final int QUEUED = 1;
    private static final int ACTIVE = 2;
    private static final int COMPLETE = 3;
    private static final int EAST = 0;
    private static final int SOUTH = 1;
    private static final int WEST = 2;
    private static final int NORTH = 3;

    private ColorContourVectorizer() {
    }

    public static List<VectorPath> vectorize(RasterFrame frame) {
        return vectorize(frame, DEFAULT_MAX_PATHS, DEFAULT_MAX_VERTICES);
    }

    public static List<VectorPath> vectorize(RasterFrame frame, int maxPaths, int maxVertices) {
        if (frame == null) {
            throw new IllegalArgumentException("frame must not be null");
        }
        return vectorize(frame.getWidth(), frame.getHeight(), frame.getOwnedPixels(), maxPaths, maxVertices);
    }

    public static List<VectorPath> vectorize(int width, int height, int[] argb) {
        return vectorize(width, height, argb, DEFAULT_MAX_PATHS, DEFAULT_MAX_VERTICES);
    }

    public static List<VectorPath> vectorize(int width, int height, int[] argb, int maxPaths, int maxVertices) {
        return vectorize(width, height, argb, maxPaths, maxVertices, false);
    }

    public static List<VectorPath> vectorizeForeground(RasterFrame frame, int maxPaths, int maxVertices) {
        if (frame == null) {
            throw new IllegalArgumentException("frame must not be null");
        }
        return vectorize(frame.getWidth(), frame.getHeight(), frame.getOwnedPixels(), maxPaths, maxVertices, true);
    }

    public static List<VectorPath> vectorizeForeground(int width, int height, int[] argb, int maxPaths, int maxVertices) {
        return vectorize(width, height, argb, maxPaths, maxVertices, true);
    }

    private static List<VectorPath> vectorize(int width, int height, int[] argb, int maxPaths, int maxVertices, boolean foregroundOnly) {
        int pixelCount = pixelLength(width, height, argb);
        validateBudgets(maxPaths, maxVertices);

        VisitState states = new VisitState(pixelCount);
        SpanStack stack = new SpanStack(stackCapacity(width, height));
        SeedBuffer topSeeds = new SeedBuffer();
        CoordinateBuffer ring = new CoordinateBuffer(Math.min(maxVertices, 64));
        VertexBudget vertexBudget = new VertexBudget(maxVertices);
        List<ColorGroup> groups = new ArrayList<ColorGroup>(Math.min(maxPaths, 1024));
        Map<Integer, ColorGroup> groupsByColor = new HashMap<Integer, ColorGroup>(Math.min(maxPaths, 1024));

        for (int pixel = 0; pixel < pixelCount; pixel++) {
            int color = argb[pixel];
            if (states.isVisited(pixel) || (color >>> 24) == 0 || foregroundOnly && color != 0xFF000000) {
                continue;
            }

            ColorGroup group = groupsByColor.get(Integer.valueOf(color));
            if (group == null) {
                if (groups.size() >= maxPaths) {
                    throw new ResourceLimitException(
                        "paths",
                        maxPaths,
                        (long)groups.size() + 1L,
                        "increase maxPaths or reduce the number of colors"
                    );
                }
                group = new ColorGroup(color);
                groups.add(group);
                groupsByColor.put(Integer.valueOf(color), group);
            }

            group.pixelArea+=floodComponent(pixel, color, width, height, argb, states, stack, topSeeds);
            traceComponent(width, height, states, group, ring, vertexBudget, topSeeds);
        }

        List<VectorPath> paths = new ArrayList<VectorPath>(groups.size());
        for (int index = 0; index < groups.size(); index++) {
            ColorGroup group = groups.get(index);
            paths.add(new VectorPath(
                PathId.of(index),
                group.rings,
                Color.fromArgb(group.argb),
                1.0,
                VectorPath.FillRule.EVEN_ODD,
                group.pixelArea
            ));
        }
        return paths;
    }

    private static int floodComponent(
        int seed,
        int color,
        int width,
        int height,
        int[] argb,
        VisitState states,
        SpanStack stack,
        SeedBuffer topSeeds
    ) {
        int pixelArea=0;
        stack.clear();
        topSeeds.clear();
        states.beginComponent();
        states.setState(seed, QUEUED);
        stack.push(seed);

        while (!stack.isEmpty()) {
            int spanSeed = stack.pop();
            if (states.getState(spanSeed) != QUEUED) {
                continue;
            }

            int y = spanSeed / width;
            int x = spanSeed - y * width;
            int rowOffset = y * width;
            int left = x;
            while (left > 0) {
                int candidate = rowOffset + left - 1;
                if (argb[candidate] != color || states.isVisited(candidate)) {
                    break;
                }
                left--;
            }

            int right = x;
            while (right + 1 < width) {
                int candidate = rowOffset + right + 1;
                if (argb[candidate] != color || states.isVisited(candidate)) {
                    break;
                }
                right++;
            }

            int first = rowOffset + left;
            int last = rowOffset + right;
            for (int pixel = first; pixel <= last; pixel++) {
                states.setState(pixel, ACTIVE);
                states.markComponent(pixel);
                pixelArea++;
                if (y == 0 || argb[pixel - width] != color) {
                    topSeeds.add(pixel);
                }
            }

            if (y > 0) {
                queueRuns(y - 1, left, right, color, width, argb, states, stack);
            }
            if (y + 1 < height) {
                queueRuns(y + 1, left, right, color, width, argb, states, stack);
            }
        }
        return pixelArea;
    }

    private static void queueRuns(
        int y,
        int left,
        int right,
        int color,
        int width,
        int[] argb,
        VisitState states,
        SpanStack stack
    ) {
        int rowOffset = y * width;
        int x = left;
        while (x <= right) {
            int candidate = rowOffset + x;
            if (argb[candidate] != color || states.getState(candidate) != UNVISITED) {
                x++;
                continue;
            }

            int start = x;
            int scan = x + 1;
            while (scan <= right) {
                int scanPixel = rowOffset + scan;
                if (argb[scanPixel] != color || states.isVisited(scanPixel)) {
                    break;
                }
                scan++;
            }

            int last = scan - 1;
            for (int index = start; index <= last; index++) {
                states.setState(rowOffset + index, QUEUED);
            }
            stack.push(rowOffset + start);
            x = scan;
        }
    }

    private static void traceComponent(
        int width,
        int height,
        VisitState states,
        ColorGroup group,
        CoordinateBuffer ring,
        VertexBudget vertexBudget,
        SeedBuffer topSeeds
    ) {
        if (topSeeds.isEmpty()) {
            throw new IllegalStateException("component seed has no top boundary");
        }
        for (int index=0;index<topSeeds.size();index++) {
            int candidate=topSeeds.get(index);
            if (!isTopBoundary(candidate, width, states)||states.isTopTraced(candidate)) {
                continue;
            }
            traceRing(candidate%width, candidate/width, width, height, states, group, ring, vertexBudget);
        }
        states.completeComponent();
    }

    private static void traceRing(
        int startX,
        int startY,
        int width,
        int height,
        VisitState states,
        ColorGroup group,
        CoordinateBuffer ring,
        VertexBudget vertexBudget
    ) {
        ring.clear();
        vertexBudget.add();
        ring.addPoint(startX, startY);

        int startPixel = startY * width + startX;
        states.markTopTraced(startPixel);
        int x = startX + 1;
        int y = startY;
        int direction = EAST;
        long steps = 0L;
        long maximumSteps = 4L * states.getPixelCount() + 4L;

        while (true) {
            int nextDirection = nextDirection(x, y, direction, width, height, states);
            if (x == startX && y == startY && nextDirection == EAST) {
                break;
            }
            if (nextDirection != direction) {
                vertexBudget.add();
                ring.addPoint(x, y);
            }

            int edgePixel = cellOnRight(x, y, nextDirection, width);
            if (nextDirection == EAST) {
                if (states.isTopTraced(edgePixel)) {
                    throw new IllegalStateException("boundary trace reused an edge");
                }
                states.markTopTraced(edgePixel);
            }
            if (nextDirection == EAST) {
                x++;
            } else if (nextDirection == SOUTH) {
                y++;
            } else if (nextDirection == WEST) {
                x--;
            } else {
                y--;
            }
            direction = nextDirection;

            steps++;
            if (steps > maximumSteps) {
                throw new IllegalStateException("boundary trace did not close");
            }
        }

        if (ring.size() < 6) {
            throw new IllegalStateException("boundary ring has fewer than three vertices");
        }
        group.rings.add(ring.toDoubleArray());
    }

    private static int nextDirection(int x, int y, int incoming, int width, int height, VisitState states) {
        int rightTurn = (incoming + 1) & 3;
        if (isBoundaryAtVertex(x, y, rightTurn, width, height, states)) {
            return rightTurn;
        }
        if (isBoundaryAtVertex(x, y, incoming, width, height, states)) {
            return incoming;
        }
        int leftTurn = (incoming + 3) & 3;
        if (isBoundaryAtVertex(x, y, leftTurn, width, height, states)) {
            return leftTurn;
        }
        throw new IllegalStateException(
            "boundary trace reached an invalid vertex at " + x + "," + y + " incoming " + incoming
        );
    }

    private static boolean isBoundaryAtVertex(
        int x,
        int y,
        int direction,
        int width,
        int height,
        VisitState states
    ) {
        int rightX;
        int rightY;
        int leftX;
        int leftY;
        if (direction == EAST) {
            rightX = x;
            rightY = y;
            leftX = x;
            leftY = y - 1;
        } else if (direction == SOUTH) {
            rightX = x - 1;
            rightY = y;
            leftX = x;
            leftY = y;
        } else if (direction == WEST) {
            rightX = x - 1;
            rightY = y - 1;
            leftX = x - 1;
            leftY = y;
        } else {
            rightX = x;
            rightY = y - 1;
            leftX = x - 1;
            leftY = y - 1;
        }
        return isComponentPixel(rightX, rightY, width, height, states)
            && !isComponentPixel(leftX, leftY, width, height, states);
    }

    private static int cellOnRight(int x, int y, int direction, int width) {
        if (direction == EAST) {
            return y * width + x;
        }
        if (direction == SOUTH) {
            return y * width + x - 1;
        }
        if (direction == WEST) {
            return (y - 1) * width + x - 1;
        }
        return (y - 1) * width + x;
    }

    private static boolean isComponentPixel(int x, int y, int width, int height, VisitState states) {
        return x >= 0 && x < width && y >= 0 && y < height && states.isComponentPixel(y * width + x);
    }

    private static boolean isTopBoundary(int pixel, int width, VisitState states) {
        return pixel < width || !states.isComponentPixel(pixel - width);
    }

    private static int pixelLength(int width, int height, int[] argb) {
        if (width <= 0) {
            throw new IllegalArgumentException("width must be positive");
        }
        if (height <= 0) {
            throw new IllegalArgumentException("height must be positive");
        }
        long length = (long) width * (long) height;
        if (length > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("pixel buffer is too large");
        }
        if (argb == null) {
            throw new IllegalArgumentException("argb must not be null");
        }
        if (argb.length != (int) length) {
            throw new IllegalArgumentException("argb length must equal width multiplied by height");
        }
        return (int) length;
    }

    private static void validateBudgets(int maxPaths, int maxVertices) {
        if (maxPaths <= 0 || maxPaths > MAX_PATHS) {
            throw new IllegalArgumentException("maxPaths must be between 1 and " + MAX_PATHS);
        }
        if (maxVertices <= 0 || maxVertices > MAX_VERTICES) {
            throw new IllegalArgumentException("maxVertices must be between 1 and " + MAX_VERTICES);
        }
    }

    private static int stackCapacity(int width, int height) {
        long runsPerRow = width / 2L + width % 2L;
        return (int) Math.min(runsPerRow * (long) height, (long) MAX_FLOOD_STACK_SPANS);
    }

    public static final class ResourceLimitException extends IllegalArgumentException {
        private static final long serialVersionUID = 1L;
        private final String resource;
        private final int limit;
        private final long required;

        private ResourceLimitException(String resource, int limit, long required, String action) {
            super(budgetName(resource) + " budget of " + limit + " exceeded; " + action + " (required at least " + required + ")");
            this.resource = resource;
            this.limit = limit;
            this.required = required;
        }

        private static String budgetName(String resource) {
            if ("paths".equals(resource)) {
                return "path";
            }
            if ("vertices".equals(resource)) {
                return "vertex";
            }
            return resource;
        }

        public String getResource() {
            return resource;
        }

        public int getLimit() {
            return limit;
        }

        public long getRequired() {
            return required;
        }
    }

    private static final class ColorGroup {
        private final int argb;
        private final List<double[]> rings = new ArrayList<double[]>(4);
        private long pixelArea;

        private ColorGroup(int argb) {
            this.argb = argb;
        }
    }

    private static final class VisitState {
        private static final int STATES_PER_WORD = 32;

        private final int pixelCount;
        private final long[] words;
        private final long[] topEdges;
        private final long[] componentBits;
        private final int[] componentWordIndices;
        private final int[] componentWordSlots;
        private final long[] componentMasks;
        private int componentWordCount;

        private VisitState(int pixelCount) {
            this.pixelCount = pixelCount;
            this.words = new long[(int) (((long) pixelCount + STATES_PER_WORD - 1L) / STATES_PER_WORD)];
            this.topEdges = new long[(int) (((long) pixelCount + 63L) / 64L)];
            this.componentBits = new long[topEdges.length];
            this.componentWordIndices = new int[words.length];
            this.componentWordSlots = new int[words.length];
            this.componentMasks = new long[words.length];
            java.util.Arrays.fill(this.componentWordSlots, -1);
        }

        private int getState(int pixel) {
            int shift = (pixel & 31) << 1;
            return (int) ((words[pixel >>> 5] >>> shift) & 3L);
        }

        private void setState(int pixel, int state) {
            int wordIndex = pixel >>> 5;
            int shift = (pixel & 31) << 1;
            long mask = 3L << shift;
            words[wordIndex] = (words[wordIndex] & ~mask) | ((long) state << shift);
        }

        private void beginComponent() {
            componentWordCount=0;
        }
        private void markComponent(int pixel) {
            int bitsetIndex=pixel>>>6;
            long bit=1L<<(pixel&63);
            if ((componentBits[bitsetIndex]&bit)!=0L) {
                return;
            }
            int stateWordIndex=pixel>>>5;
            int slot=componentWordSlots[stateWordIndex];
            if (slot<0) {
                slot=componentWordCount++;
                componentWordIndices[slot]=stateWordIndex;
                componentMasks[slot]=0L;
                componentWordSlots[stateWordIndex]=slot;
            }
            componentMasks[slot]|=1L<<(pixel&31);
            componentBits[bitsetIndex]|=bit;
        }
        private boolean isComponentPixel(int pixel) {
            return (componentBits[pixel>>>6]&(1L<<(pixel&63)))!=0L;
        }
        private void completeComponent() {
            for (int slot=0;slot<componentWordCount;slot++) {
                int stateWordIndex=componentWordIndices[slot];
                long mask=componentMasks[slot];
                long remaining=mask;
                while (remaining!=0L) {
                    int bit=Long.numberOfTrailingZeros(remaining);
                    int pixel=(stateWordIndex<<5)+bit;
                    setState(pixel, COMPLETE);
                    clearTopTraced(pixel);
                    remaining&=remaining-1L;
                }
                int bitsetIndex=stateWordIndex>>>1;
                long clearMask=mask<<((stateWordIndex&1)*32);
                componentBits[bitsetIndex]&=~clearMask;
                componentWordSlots[stateWordIndex]=-1;
            }
            componentWordCount=0;
        }
        private boolean isVisited(int pixel) {
            return getState(pixel) >= ACTIVE;
        }

        private void markTopTraced(int pixel) {
            topEdges[pixel >>> 6] |= 1L << (pixel & 63);
        }

        private boolean isTopTraced(int pixel) {
            return (topEdges[pixel >>> 6] & (1L << (pixel & 63))) != 0L;
        }

        private void clearTopTraced(int pixel) {
            topEdges[pixel >>> 6] &= ~(1L << (pixel & 63));
        }

        private int getPixelCount() {
            return pixelCount;
        }
    }

    private static final class SeedBuffer {
        private int[] values=new int[64];
        private int size;
        private void clear() {
            size=0;
        }
        private void add(int value) {
            if (size==values.length) {
                int[] expanded=new int[values.length<<1];
                System.arraycopy(values,0,expanded,0,values.length);
                values=expanded;
            }
            values[size++]=value;
        }
        private int get(int index) {
            return values[index];
        }
        private int size() {
            return size;
        }
        private boolean isEmpty() {
            return size==0;
        }
    }

    private static final class SpanStack {
        private final int[] spans;
        private int size;

        private SpanStack(int capacity) {
            this.spans = new int[capacity];
        }

        private void clear() {
            size = 0;
        }

        private boolean isEmpty() {
            return size == 0;
        }

        private int pop() {
            return spans[--size];
        }

        private void push(int span) {
            if (size == spans.length) {
                throw new ResourceLimitException(
                    "flood-stack-spans",
                    MAX_FLOOD_STACK_SPANS,
                    (long) size + 1L,
                    "reduce raster complexity or dimensions"
                );
            }
            spans[size++] = span;
        }
    }

    private static final class CoordinateBuffer {
        private int[] coordinates;
        private int size;

        private CoordinateBuffer(int pointCapacity) {
            coordinates = new int[Math.max(pointCapacity, 1) * 2];
        }

        private void clear() {
            size = 0;
        }

        private void addPoint(int x, int y) {
            if (size == coordinates.length) {
                int newLength = coordinates.length << 1;
                int[] expanded = new int[newLength];
                System.arraycopy(coordinates, 0, expanded, 0, coordinates.length);
                coordinates = expanded;
            }
            coordinates[size++] = x;
            coordinates[size++] = y;
        }

        private int size() {
            return size;
        }

        private double[] toDoubleArray() {
            double[] result = new double[size];
            for (int index = 0; index < size; index++) {
                result[index] = coordinates[index];
            }
            return result;
        }
    }

    private static final class VertexBudget {
        private final int limit;
        private int used;

        private VertexBudget(int limit) {
            this.limit = limit;
        }

        private void add() {
            if (used == limit) {
                throw new ResourceLimitException(
                    "vertices",
                    limit,
                    (long) used + 1L,
                    "increase maxVertices or simplify the raster"
                );
            }
            used++;
        }
    }
}
