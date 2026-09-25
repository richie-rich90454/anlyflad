package com.anlyflad.core.raster;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import com.anlyflad.core.perf.ColorLut;
public final class PixelQuantizerTest {
    @Test
    public void shouldUseLutTieRuleAndPreserveSourceAlpha() {
        PixelQuantizer quantizer=new PixelQuantizer(new int[] {0x000083, 0x000085});
        int[] pixels={0x80800000};
        quantizer.quantize(pixels);
        assertArrayEquals(new int[] {0x80000083}, pixels);
        PixelQuantizer fromLut=new PixelQuantizer(new ColorLut(new int[] {0x00102030}));
        int[] exact={0xAB102030};
        fromLut.quantize(exact);
        assertArrayEquals(new int[] {0xAB102030}, exact);
    }
    @Test
    public void shouldBeIndependentOfLaterPaletteMutation() {
        int[] palette={0x00FFFFFF, 0x00000000};
        PixelQuantizer quantizer=new PixelQuantizer(palette);
        palette[0]=0x00000000;
        palette[1]=0x00FFFFFF;
        int[] pixels={0xFF000000, 0xFFFFFFFF};
        quantizer.quantize(pixels);
        assertArrayEquals(new int[] {0xFF000000, 0xFFFFFFFF}, pixels);
    }
    @Test
    public void shouldRejectNullPaletteLutAndPixels() {
        try {
            new PixelQuantizer((int[]) null);
            fail("Expected null palette to be rejected");
        } catch (IllegalArgumentException exception) {
            assertTrue(exception.getMessage().length()>0);
        }
        try {
            new PixelQuantizer((ColorLut) null);
            fail("Expected null lut to be rejected");
        } catch (IllegalArgumentException exception) {
            assertTrue(exception.getMessage().length()>0);
        }
        PixelQuantizer quantizer=new PixelQuantizer(new int[] {0x000000});
        try {
            quantizer.quantize(null);
            fail("Expected null pixels to be rejected");
        } catch (IllegalArgumentException exception) {
            assertTrue(exception.getMessage().length()>0);
        }
    }
}
