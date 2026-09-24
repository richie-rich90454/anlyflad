package com.vectorium.core.raster;
public final class RasterPreprocessor {
    private static final int MIN_BRIGHTNESS=-255;
    private static final int MAX_BRIGHTNESS=255;
    private static final double MIN_CONTRAST=0.0;
    private static final double MAX_CONTRAST=2.0;
    private RasterPreprocessor() {
    }
    public static void brightness(int[] rgba, int amount) {
        validatePixels(rgba);
        if (amount<MIN_BRIGHTNESS||amount>MAX_BRIGHTNESS) {
            throw new IllegalArgumentException("brightness must be between -255 and 255");
        }
        for (int index=0;index<rgba.length;index++) {
            int pixel=rgba[index];
            int alpha=pixel&0xFF000000;
            int red=clampChannel(((pixel>>>16)&0xFF)+amount);
            int green=clampChannel(((pixel>>>8)&0xFF)+amount);
            int blue=clampChannel((pixel&0xFF)+amount);
            rgba[index]=alpha|(red<<16)|(green<<8)|blue;
        }
    }
    public static void contrast(int[] rgba, double factor) {
        validatePixels(rgba);
        if (!Double.isFinite(factor)||factor<MIN_CONTRAST||factor>MAX_CONTRAST) {
            throw new IllegalArgumentException("contrast must be finite and between 0 and 2");
        }
        for (int index=0;index<rgba.length;index++) {
            int pixel=rgba[index];
            int alpha=pixel&0xFF000000;
            int red=adjustContrast((pixel>>>16)&0xFF, factor);
            int green=adjustContrast((pixel>>>8)&0xFF, factor);
            int blue=adjustContrast(pixel&0xFF, factor);
            rgba[index]=alpha|(red<<16)|(green<<8)|blue;
        }
    }
    public static void grayscale(int[] rgba) {
        validatePixels(rgba);
        for (int index=0;index<rgba.length;index++) {
            int pixel=rgba[index];
            int alpha=pixel&0xFF000000;
            int gray=luminance((pixel>>>16)&0xFF, (pixel>>>8)&0xFF, pixel&0xFF);
            rgba[index]=alpha|(gray<<16)|(gray<<8)|gray;
        }
    }
    private static int adjustContrast(int channel, double factor) {
        int adjusted=(int)Math.round((channel-128)*factor+128.0);
        return clampChannel(adjusted);
    }
    private static int luminance(int red, int green, int blue) {
        return (red*77+green*150+blue*29)>>>8;
    }
    private static int clampChannel(int channel) {
        if (channel<0) {
            return 0;
        }
        if (channel>255) {
            return 255;
        }
        return channel;
    }
    private static void validatePixels(int[] rgba) {
        if (rgba==null) {
            throw new IllegalArgumentException("rgba must not be null");
        }
    }
}
