package com.anlyflad.core.raster;
import java.util.Arrays;
public final class RasterFrame {
    private final int width;
    private final int height;
    private final int[] pixels;
    public RasterFrame(int width, int height, int[] rgba) {
        this(width, height, rgba, true);
    }
    private RasterFrame(int width, int height, int[] rgba, boolean copyPixels) {
        int pixelLength=pixelLength(width, height);
        if (rgba==null) {
            throw new IllegalArgumentException("rgba must not be null");
        }
        if (rgba.length!=pixelLength) {
            throw new IllegalArgumentException("rgba length must equal width multiplied by height");
        }
        this.width=width;
        this.height=height;
        this.pixels=copyPixels?Arrays.copyOf(rgba, rgba.length):rgba;
    }
    public static RasterFrame of(int width, int height, int[] rgba) {
        return new RasterFrame(width, height, rgba, true);
    }
    public static RasterFrame wrap(int width, int height, int[] rgba) {
        return new RasterFrame(width, height, rgba, false);
    }
    public int getWidth() {
        return width;
    }
    public int getHeight() {
        return height;
    }
    public int getPixel(int x, int y) {
        return pixels[pixelIndex(x, y)];
    }
    public void setPixel(int x, int y, int rgba) {
        pixels[pixelIndex(x, y)]=rgba;
    }
    public int[] getPixels() {
        return Arrays.copyOf(pixels, pixels.length);
    }
    public int[] copyPixels() {
        return getPixels();
    }
    public int[] getOwnedPixels() {
        return pixels;
    }
    public RasterFrame copy() {
        return new RasterFrame(width, height, pixels, true);
    }
    private int pixelIndex(int x, int y) {
        if (x<0||x>=width) {
            throw new IllegalArgumentException("x is outside the frame");
        }
        if (y<0||y>=height) {
            throw new IllegalArgumentException("y is outside the frame");
        }
        return y*width+x;
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
