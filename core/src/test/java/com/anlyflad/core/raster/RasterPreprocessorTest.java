package com.anlyflad.core.raster;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
public final class RasterPreprocessorTest {
    @Test
    public void shouldAdjustBrightnessClampRgbAndPreserveAlpha() {
        int[] pixels={0x80102030, 0xFF102030, 0xFFFFFFFF};
        RasterPreprocessor.brightness(pixels, 20);
        assertArrayEquals(new int[] {0x80243444, 0xFF243444, 0xFFFFFFFF}, pixels);
        int[] darkPixels={0x80102030, 0xFF102030};
        RasterPreprocessor.brightness(darkPixels, -20);
        assertArrayEquals(new int[] {0x80000C1C, 0xFF000C1C}, darkPixels);
    }
    @Test
    public void shouldApplyContrastAndGrayscaleWithoutChangingAlpha() {
        int[] contrastPixels={0xFF000000, 0x80108080, 0xFFFF0000};
        RasterPreprocessor.contrast(contrastPixels, 0.0);
        assertArrayEquals(new int[] {0xFF808080, 0x80808080, 0xFF808080}, contrastPixels);
        int[] grayscalePixels={0xFF102040, 0x80102040};
        RasterPreprocessor.grayscale(grayscalePixels);
        assertArrayEquals(new int[] {0xFF1E1E1E, 0x801E1E1E}, grayscalePixels);
    }
    @Test
    public void shouldRejectInvalidPreprocessorArguments() {
        try {
            RasterPreprocessor.brightness(null, 0);
            fail("Expected null pixels to be rejected");
        } catch (IllegalArgumentException exception) {
            assertTrue(exception.getMessage().length()>0);
        }
        try {
            RasterPreprocessor.brightness(new int[0], -256);
            fail("Expected low brightness to be rejected");
        } catch (IllegalArgumentException exception) {
            assertTrue(exception.getMessage().length()>0);
        }
        try {
            RasterPreprocessor.brightness(new int[0], 256);
            fail("Expected high brightness to be rejected");
        } catch (IllegalArgumentException exception) {
            assertTrue(exception.getMessage().length()>0);
        }
        try {
            RasterPreprocessor.contrast(new int[0], -0.1);
            fail("Expected low contrast to be rejected");
        } catch (IllegalArgumentException exception) {
            assertTrue(exception.getMessage().length()>0);
        }
        try {
            RasterPreprocessor.contrast(new int[0], 2.1);
            fail("Expected high contrast to be rejected");
        } catch (IllegalArgumentException exception) {
            assertTrue(exception.getMessage().length()>0);
        }
        try {
            RasterPreprocessor.contrast(new int[0], Double.NaN);
            fail("Expected nonfinite contrast to be rejected");
        } catch (IllegalArgumentException exception) {
            assertTrue(exception.getMessage().length()>0);
        }
        try {
            RasterPreprocessor.grayscale(null);
            fail("Expected null grayscale pixels to be rejected");
        } catch (IllegalArgumentException exception) {
            assertTrue(exception.getMessage().length()>0);
        }
    }
}
