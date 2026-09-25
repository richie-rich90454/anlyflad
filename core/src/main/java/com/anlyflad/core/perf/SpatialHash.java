package com.anlyflad.core.perf;
public final class SpatialHash {
    private final double minX;
    private final double minY;
    private final double maxX;
    private final double maxY;
    private final int gridWidth;
    private final int gridHeight;
    private final double cellWidth;
    private final double cellHeight;
    private final int[] heads;
    private final int[] next;
    private final int[] itemIds;
    private final int capacity;
    private int used;
    public SpatialHash(double minX, double minY, double maxX, double maxY, int gridSize, int capacity) {
        this(minX, minY, maxX, maxY, gridSize, gridSize, capacity);
    }
    public SpatialHash(double minX, double minY, double maxX, double maxY, int gridWidth, int gridHeight, int capacity) {
        validateFinite(minX);
        validateFinite(minY);
        validateFinite(maxX);
        validateFinite(maxY);
        if (minX>=maxX||minY>=maxY) {
            throw new IllegalArgumentException("bounds must have positive area");
        }
        if (gridWidth<=0||gridHeight<=0||capacity<=0) {
            throw new IllegalArgumentException("grid dimensions and capacity must be positive");
        }
        double width=maxX-minX;
        double height=maxY-minY;
        double widthStep=width/gridWidth;
        double heightStep=height/gridHeight;
        if (!Double.isFinite(widthStep)||!Double.isFinite(heightStep)||widthStep<=0.0||heightStep<=0.0) {
            throw new IllegalArgumentException("grid cell size must be finite and positive");
        }
        long cellCount=(long)gridWidth*gridHeight;
        if (cellCount>Integer.MAX_VALUE) {
            throw new IllegalArgumentException("grid is too large");
        }
        this.minX=minX;
        this.minY=minY;
        this.maxX=maxX;
        this.maxY=maxY;
        this.gridWidth=gridWidth;
        this.gridHeight=gridHeight;
        this.cellWidth=widthStep;
        this.cellHeight=heightStep;
        this.heads=new int[(int)cellCount];
        this.next=new int[capacity];
        this.itemIds=new int[capacity];
        this.capacity=capacity;
        this.used=0;
        clear();
    }
    public SpatialHash(double[] bounds, int gridSize, int capacity) {
        this(validateBounds(bounds)[0], validateBounds(bounds)[1], validateBounds(bounds)[2], validateBounds(bounds)[3], gridSize, gridSize, capacity);
    }
    public void clear() {
        for (int index=0;index<heads.length;index++) {
            heads[index]=-1;
        }
        used=0;
    }
    public boolean insert(int itemId, double itemMinX, double itemMinY, double itemMaxX, double itemMaxY) {
        if (itemId<0) {
            throw new IllegalArgumentException("itemId must be nonnegative");
        }
        validateFinite(itemMinX);
        validateFinite(itemMinY);
        validateFinite(itemMaxX);
        validateFinite(itemMaxY);
        if (itemMinX>itemMaxX||itemMinY>itemMaxY) {
            throw new IllegalArgumentException("item bounds are inverted");
        }
        if (itemMaxX<minX||itemMinX>maxX||itemMaxY<minY||itemMinY>maxY) {
            return false;
        }
        int firstColumn=columnFor(itemMinX);
        int lastColumn=columnFor(itemMaxX);
        int firstRow=rowFor(itemMinY);
        int lastRow=rowFor(itemMaxY);
        long membershipCount=(long)(lastColumn-firstColumn+1)*(lastRow-firstRow+1);
        if (membershipCount>capacity-used) {
            return false;
        }
        int slot=used;
        for (int row=firstRow;row<=lastRow;row++) {
            for (int column=firstColumn;column<=lastColumn;column++) {
                int cell=row*gridWidth+column;
                itemIds[slot]=itemId;
                next[slot]=heads[cell];
                heads[cell]=slot;
                slot++;
            }
        }
        used=slot;
        return true;
    }
    public boolean insert(int itemId, double x, double y) {
        return insert(itemId, x, y, x, y);
    }
    public int query(double queryMinX, double queryMinY, double queryMaxX, double queryMaxY, int[] output) {
        validateFinite(queryMinX);
        validateFinite(queryMinY);
        validateFinite(queryMaxX);
        validateFinite(queryMaxY);
        if (queryMinX>queryMaxX||queryMinY>queryMaxY) {
            throw new IllegalArgumentException("query bounds are inverted");
        }
        if (output==null) {
            throw new IllegalArgumentException("output must not be null");
        }
        if (queryMaxX<minX||queryMinX>maxX||queryMaxY<minY||queryMinY>maxY) {
            return 0;
        }
        int firstColumn=columnFor(queryMinX);
        int lastColumn=columnFor(queryMaxX);
        int firstRow=rowFor(queryMinY);
        int lastRow=rowFor(queryMaxY);
        int count=0;
        for (int row=firstRow;row<=lastRow;row++) {
            for (int column=firstColumn;column<=lastColumn;column++) {
                int cell=row*gridWidth+column;
                int slot=heads[cell];
                while (slot!=-1) {
                    if (count==output.length) {
                        throw new IllegalArgumentException("query output is exhausted");
                    }
                    output[count]=itemIds[slot];
                    count++;
                    slot=next[slot];
                }
            }
        }
        return count;
    }
    public int query(double x, double y, int[] output) {
        return query(x, y, x, y, output);
    }
    public int getUsedCapacity() {
        return used;
    }
    public int getCellCount() {
        return heads.length;
    }
    private int columnFor(double value) {
        if (value<=minX) {
            return 0;
        }
        if (value>=maxX) {
            return gridWidth-1;
        }
        double position=(value-minX)/cellWidth;
        if (!Double.isFinite(position)) {
            throw new IllegalArgumentException("coordinate cannot be mapped to the grid");
        }
        int column=(int)position;
        if (column<0) {
            return 0;
        }
        if (column>=gridWidth) {
            return gridWidth-1;
        }
        return column;
    }
    private int rowFor(double value) {
        if (value<=minY) {
            return 0;
        }
        if (value>=maxY) {
            return gridHeight-1;
        }
        double position=(value-minY)/cellHeight;
        if (!Double.isFinite(position)) {
            throw new IllegalArgumentException("coordinate cannot be mapped to the grid");
        }
        int row=(int)position;
        if (row<0) {
            return 0;
        }
        if (row>=gridHeight) {
            return gridHeight-1;
        }
        return row;
    }
    private static double[] validateBounds(double[] bounds) {
        if (bounds==null||bounds.length!=4) {
            throw new IllegalArgumentException("bounds must contain four coordinates");
        }
        validateFinite(bounds[0]);
        validateFinite(bounds[1]);
        validateFinite(bounds[2]);
        validateFinite(bounds[3]);
        return bounds;
    }
    private static void validateFinite(double value) {
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException("coordinates must be finite");
        }
    }
}
