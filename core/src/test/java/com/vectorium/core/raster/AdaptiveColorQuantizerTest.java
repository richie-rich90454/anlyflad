package com.vectorium.core.raster;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

public final class AdaptiveColorQuantizerTest {
    @Test
    public void shouldBuildDeterministicBoundedPaletteAndPreserveAlpha() {
        int[] pixels=new int[64];
        for (int index=0;index<pixels.length;index++) {
            pixels[index]=0x80000000|(index*37&0xFFFFFF);
        }
        pixels[0]=0x00ABCDEF;
        AdaptiveColorQuantizer quantizer=new AdaptiveColorQuantizer(8);
        int[] firstPalette=quantizer.buildPalette(pixels);
        int[] secondPalette=quantizer.buildPalette(pixels);
        int[] firstPixels=Arrays.copyOf(pixels,pixels.length);
        int[] secondPixels=Arrays.copyOf(pixels,pixels.length);
        quantizer.quantize(firstPixels);
        quantizer.quantize(secondPixels);
        assertArrayEquals(firstPalette,secondPalette);
        assertArrayEquals(firstPixels,secondPixels);
        assertTrue(firstPalette.length>0);
        assertTrue(firstPalette.length<=8);
        Set<Integer> colors=new HashSet<Integer>();
        for (int index=1;index<firstPixels.length;index++) {
            assertEquals(0x80,firstPixels[index]>>>24);
            colors.add(Integer.valueOf(firstPixels[index]&0xFFFFFF));
        }
        assertTrue(colors.size()<=8);
        assertEquals(0x00ABCDEF,firstPixels[0]);
    }

    @Test
    public void shouldReduceManyColorsToConfiguredLimit() {
        int[] pixels=new int[512];
        for (int index=0;index<pixels.length;index++) {
            pixels[index]=0xFF000000|index*0x00010101;
        }
        int[] quantized=Arrays.copyOf(pixels,pixels.length);
        new AdaptiveColorQuantizer(4).quantize(quantized);
        Set<Integer> colors=new HashSet<Integer>();
        for (int index=0;index<quantized.length;index++) {
            colors.add(Integer.valueOf(quantized[index]));
        }
        assertTrue(colors.size()<=4);
    }

    @Test
    public void shouldRejectInvalidSettingsAndNullPixels() {
        assertThrows(IllegalArgumentException.class,() -> new AdaptiveColorQuantizer(0));
        assertThrows(IllegalArgumentException.class,() -> new AdaptiveColorQuantizer(AdaptiveColorQuantizer.MAX_COLORS+1));
        assertThrows(IllegalArgumentException.class,() -> new AdaptiveColorQuantizer(1).buildPalette(null));
    }
}
