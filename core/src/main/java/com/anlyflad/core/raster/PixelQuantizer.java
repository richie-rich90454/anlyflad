package com.anlyflad.core.raster;
import com.anlyflad.core.perf.ColorLut;
public final class PixelQuantizer {
    private final ColorLut lut;
    public PixelQuantizer(int[] palette) {
        this.lut=new ColorLut(palette);
    }
    public PixelQuantizer(ColorLut lut) {
        if (lut==null) {
            throw new IllegalArgumentException("lut must not be null");
        }
        this.lut=lut;
    }
    public void quantize(int[] rgba) {
        if (rgba==null) {
            throw new IllegalArgumentException("rgba must not be null");
        }
        for (int index=0;index<rgba.length;index++) {
            rgba[index]=lut.lookupArgb(rgba[index]);
        }
    }
}
