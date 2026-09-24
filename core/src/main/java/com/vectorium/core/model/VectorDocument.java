package com.vectorium.core.model;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
public final class VectorDocument {
    private final String documentId;
    private final Origin origin;
    private final List<VectorPath> paths;
    private final int width;
    private final int height;
    private final int[] pixels;
    public VectorDocument(String documentId, Origin origin, List<VectorPath> paths, int width, int height, int[] pixels) {
        if (documentId==null||documentId.trim().isEmpty()) {
            throw new IllegalArgumentException("documentId must not be blank");
        }
        if (origin==null) {
            throw new IllegalArgumentException("origin must not be null");
        }
        if (paths==null) {
            throw new IllegalArgumentException("paths must not be null");
        }
        if (pixels==null) {
            throw new IllegalArgumentException("pixels must not be null");
        }
        validateSize(width, height);
        if (pixels.length!=pixelLength(width, height)) {
            throw new IllegalArgumentException("pixels length must equal width multiplied by height");
        }
        List<VectorPath> copiedPaths=new ArrayList<VectorPath>(paths.size());
        for (int index=0;index<paths.size();index++) {
            VectorPath path=paths.get(index);
            if (path==null) {
                throw new IllegalArgumentException("paths must not contain null");
            }
            copiedPaths.add(path);
        }
        this.documentId=documentId;
        this.origin=origin;
        this.paths=Collections.unmodifiableList(copiedPaths);
        this.width=width;
        this.height=height;
        this.pixels=Arrays.copyOf(pixels, pixels.length);
    }
    public static VectorDocument emptySvg(String documentId, int width, int height) {
        return new VectorDocument(documentId, new SvgOrigin(documentId), Collections.<VectorPath>emptyList(), width, height, new int[pixelLength(width, height)]);
    }
    public static VectorDocument emptyRaster(String documentId, int width, int height) {
        return new VectorDocument(documentId, new RasterOrigin(documentId), Collections.<VectorPath>emptyList(), width, height, new int[pixelLength(width, height)]);
    }
    public String getDocumentId() {
        return documentId;
    }
    public Origin getOrigin() {
        return origin;
    }
    public List<VectorPath> getPaths() {
        return paths;
    }
    public int getWidth() {
        return width;
    }
    public int getHeight() {
        return height;
    }
    public int[] getOwnedPixels() {
        return pixels;
    }
    public VectorDocument withPaths(List<VectorPath> paths) {
        return new VectorDocument(documentId, origin, paths, width, height, pixels);
    }
    public VectorDocument withPixels(int[] pixels) {
        return new VectorDocument(documentId, origin, paths, width, height, pixels);
    }
    public VectorDocument withSize(int width, int height) {
        validateSize(width, height);
        int[] resizedPixels=Arrays.copyOf(pixels, pixelLength(width, height));
        return new VectorDocument(documentId, origin, paths, width, height, resizedPixels);
    }
    public VectorDocument withOrigin(Origin origin) {
        return new VectorDocument(documentId, origin, paths, width, height, pixels);
    }
    @Override
    public boolean equals(Object other) {
        if (this==other) {
            return true;
        }
        if (other==null||getClass()!=other.getClass()) {
            return false;
        }
        VectorDocument document=(VectorDocument)other;
        return width==document.width&&height==document.height&&documentId.equals(document.documentId)&&origin.equals(document.origin)&&paths.equals(document.paths)&&Arrays.equals(pixels, document.pixels);
    }
    @Override
    public int hashCode() {
        int result=17;
        result=31*result+documentId.hashCode();
        result=31*result+origin.hashCode();
        result=31*result+paths.hashCode();
        result=31*result+width;
        result=31*result+height;
        result=31*result+Arrays.hashCode(pixels);
        return result;
    }
    @Override
    public String toString() {
        return "VectorDocument{documentId="+documentId+", origin="+origin+", paths="+paths+", width="+width+", height="+height+", pixelLength="+pixels.length+"}";
    }
    private static void validateSize(int width, int height) {
        if (width<0) {
            throw new IllegalArgumentException("width must be nonnegative");
        }
        if (height<0) {
            throw new IllegalArgumentException("height must be nonnegative");
        }
        pixelLength(width, height);
    }
    private static int pixelLength(int width, int height) {
        long length=(long)width*(long)height;
        if (length>Integer.MAX_VALUE) {
            throw new IllegalArgumentException("pixel buffer is too large");
        }
        return (int)length;
    }
}
