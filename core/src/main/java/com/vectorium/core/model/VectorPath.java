package com.vectorium.core.model;
import java.util.Arrays;
public final class VectorPath {
    private final PathId id;
    private final double[] coordinates;
    private final boolean closed;
    private final Color fill;
    private final double opacity;
    private final Rect bounds;
    private final double area;
    private final int nodeCount;
    public VectorPath(PathId id, double[] coordinates, boolean closed, Color fill, double opacity) {
        if (id==null) {
            throw new IllegalArgumentException("id must not be null");
        }
        if (coordinates==null) {
            throw new IllegalArgumentException("coordinates must not be null");
        }
        if (fill==null) {
            throw new IllegalArgumentException("fill must not be null");
        }
        if (coordinates.length==0||coordinates.length%2!=0) {
            throw new IllegalArgumentException("coordinates must contain at least one x and y pair");
        }
        for (int index=0;index<coordinates.length;index++) {
            if (!Double.isFinite(coordinates[index])) {
                throw new IllegalArgumentException("coordinates must be finite");
            }
        }
        if (!Double.isFinite(opacity)||opacity<0.0||opacity>1.0) {
            throw new IllegalArgumentException("opacity must be between 0 and 1");
        }
        this.id=id;
        this.coordinates=Arrays.copyOf(coordinates, coordinates.length);
        this.closed=closed;
        this.fill=fill;
        this.opacity=opacity;
        this.bounds=calculateBounds(this.coordinates);
        this.area=calculateArea(this.coordinates, closed);
        this.nodeCount=this.coordinates.length/2;
    }
    public PathId getId() {
        return id;
    }
    public double[] getCoordinates() {
        return Arrays.copyOf(coordinates, coordinates.length);
    }
    public boolean isClosed() {
        return closed;
    }
    public Color getFill() {
        return fill;
    }
    public double getOpacity() {
        return opacity;
    }
    public Rect getBounds() {
        return bounds;
    }
    public double getArea() {
        return area;
    }
    public int getNodeCount() {
        return nodeCount;
    }
    public VectorPath withGeometry(double[] coordinates, boolean closed) {
        return new VectorPath(id, coordinates, closed, fill, opacity);
    }
    public VectorPath withStyle(Color fill, double opacity) {
        return new VectorPath(id, coordinates, closed, fill, opacity);
    }
    @Override
    public boolean equals(Object other) {
        if (this==other) {
            return true;
        }
        if (other==null||getClass()!=other.getClass()) {
            return false;
        }
        VectorPath path=(VectorPath)other;
        return closed==path.closed&&Double.doubleToLongBits(opacity)==Double.doubleToLongBits(path.opacity)&&id.equals(path.id)&&Arrays.equals(coordinates, path.coordinates)&&fill.equals(path.fill);
    }
    @Override
    public int hashCode() {
        int result=17;
        result=31*result+id.hashCode();
        result=31*result+Arrays.hashCode(coordinates);
        result=31*result+(closed?1:0);
        result=31*result+fill.hashCode();
        result=31*result+longBits(opacity);
        result=31*result+bounds.hashCode();
        result=31*result+longBits(area);
        result=31*result+nodeCount;
        return result;
    }
    @Override
    public String toString() {
        return "VectorPath{id="+id+", coordinates="+Arrays.toString(coordinates)+", closed="+closed+", fill="+fill+", opacity="+opacity+", bounds="+bounds+", area="+area+", nodeCount="+nodeCount+"}";
    }
    private static Rect calculateBounds(double[] coordinates) {
        double minX=coordinates[0];
        double minY=coordinates[1];
        double maxX=coordinates[0];
        double maxY=coordinates[1];
        for (int index=2;index<coordinates.length;index+=2) {
            double x=coordinates[index];
            double y=coordinates[index+1];
            minX=Math.min(minX, x);
            minY=Math.min(minY, y);
            maxX=Math.max(maxX, x);
            maxY=Math.max(maxY, y);
        }
        return new Rect(minX, minY, maxX, maxY);
    }
    private static double calculateArea(double[] coordinates, boolean closed) {
        if (!closed) {
            return 0.0;
        }
        double sum=0.0;
        for (int index=0;index<coordinates.length;index+=2) {
            int nextIndex=(index+2)%coordinates.length;
            sum+=coordinates[index]*coordinates[nextIndex+1]-coordinates[nextIndex]*coordinates[index+1];
        }
        return Math.abs(sum)*0.5;
    }
    private static int longBits(double value) {
        long bits=Double.doubleToLongBits(value);
        return (int)(bits^(bits>>>32));
    }
}
