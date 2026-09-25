package com.anlyflad.core.raster;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
public final class ThresholdServiceTest {
    private static final int BLACK=0xFF000000;
    private static final int WHITE=0xFFFFFFFF;
    @Test
    public void shouldThresholdLuminanceAndForceOpaqueColors() {
        int[] pixels={argb(0x80, 0, 0, 0), argb(0x40, 255, 255, 255), argb(0, 128, 128, 128), argb(0, 127, 127, 127)};
        ThresholdService.apply(pixels, 128);
        assertArrayEquals(new int[] {BLACK, WHITE, WHITE, WHITE}, pixels);
    }
    @Test
    public void shouldAcceptThresholdBounds() {
        int[] pixels={0xFF000000, 0xFFFFFFFF};
        ThresholdService.apply(pixels, 0);
        assertArrayEquals(new int[] {WHITE, WHITE}, pixels);
        pixels[0]=0xFF101010;
        ThresholdService.apply(pixels, 255);
        assertArrayEquals(new int[] {BLACK, WHITE}, pixels);
    }
    @Test
    public void shouldRejectInvalidThresholdArguments() {
        try {
            ThresholdService.apply(null, 0);
            fail("Expected null pixels to be rejected");
        } catch (IllegalArgumentException exception) {
            assertTrue(exception.getMessage().length()>0);
        }
        try {
            ThresholdService.apply(new int[0], -1);
            fail("Expected low threshold to be rejected");
        } catch (IllegalArgumentException exception) {
            assertTrue(exception.getMessage().length()>0);
        }
        try {
            ThresholdService.apply(new int[0], 256);
            fail("Expected high threshold to be rejected");
        } catch (IllegalArgumentException exception) {
            assertTrue(exception.getMessage().length()>0);
        }
    }
    private static int argb(int alpha, int red, int green, int blue) {
        return (alpha<<24)|(red<<16)|(green<<8)|blue;
    }
}
