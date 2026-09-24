package com.vectorium.core.raster;
public final class ThresholdService {
    private static final int BLACK=0xFF000000;
    private static final int WHITE=0xFFFFFFFF;
    private ThresholdService() {
    }
    public static void apply(int[] rgba, int threshold) {
        if (rgba==null) {
            throw new IllegalArgumentException("rgba must not be null");
        }
        if (threshold<0||threshold>255) {
            throw new IllegalArgumentException("threshold must be between 0 and 255");
        }
        for (int index=0;index<rgba.length;index++) {
            int pixel=rgba[index];
            int luminance=luminance((pixel>>>16)&0xFF, (pixel>>>8)&0xFF, pixel&0xFF);
            if (luminance<threshold) {
                rgba[index]=BLACK;
            } else {
                rgba[index]=WHITE;
            }
        }
    }
    private static int luminance(int red, int green, int blue) {
        return (red*77+green*150+blue*29)>>>8;
    }
}
