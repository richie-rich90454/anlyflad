package com.anlyflad.core.raster;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
public final class RasterFrameTest {
    @Test
    public void shouldCopyDefensivelyAndPreserveRgbaValues() {
        int[] source={0x80112233, 0xFF445566};
        RasterFrame frame=RasterFrame.of(2, 1, source);
        assertEquals(2, frame.getWidth());
        assertEquals(1, frame.getHeight());
        assertEquals(0x80112233, frame.getPixel(0, 0));
        assertEquals(0xFF445566, frame.getPixel(1, 0));
        assertNotSame(source, frame.getOwnedPixels());
        source[0]=0;
        assertEquals(0x80112233, frame.getPixel(0, 0));
        int[] exposed=frame.getPixels();
        exposed[0]=0;
        assertEquals(0x80112233, frame.getPixel(0, 0));
        assertArrayEquals(new int[] {0x80112233, 0xFF445566}, frame.copyPixels());
        RasterFrame copy=frame.copy();
        assertNotSame(frame, copy);
        assertNotSame(frame.getOwnedPixels(), copy.getOwnedPixels());
        assertArrayEquals(frame.getPixels(), copy.getPixels());
        frame.setPixel(1, 0, 0x7F010203);
        assertEquals(0xFF445566, copy.getPixel(1, 0));
    }
    @Test
    public void shouldWrapAnOwnedBufferWithoutCopying() {
        int[] owned={0xFF000000, 0xFFFFFFFF};
        RasterFrame frame=RasterFrame.wrap(1, 2, owned);
        assertSame(owned, frame.getOwnedPixels());
        owned[0]=0x80112233;
        assertEquals(0x80112233, frame.getPixel(0, 0));
    }
    @Test
    public void shouldRejectInvalidDimensionsBuffersAndCoordinates() {
        try {
            RasterFrame.of(0, 1, new int[0]);
            fail("Expected zero width to be rejected");
        } catch (IllegalArgumentException exception) {
            assertTrue(exception.getMessage().length()>0);
        }
        try {
            RasterFrame.of(1, -1, new int[0]);
            fail("Expected negative height to be rejected");
        } catch (IllegalArgumentException exception) {
            assertTrue(exception.getMessage().length()>0);
        }
        try {
            RasterFrame.of(2, 2, new int[3]);
            fail("Expected mismatched buffer to be rejected");
        } catch (IllegalArgumentException exception) {
            assertTrue(exception.getMessage().length()>0);
        }
        try {
            RasterFrame.of(1, 1, null);
            fail("Expected null buffer to be rejected");
        } catch (IllegalArgumentException exception) {
            assertTrue(exception.getMessage().length()>0);
        }
        try {
            RasterFrame.of(Integer.MAX_VALUE, 2, new int[0]);
            fail("Expected overflowing dimensions to be rejected");
        } catch (IllegalArgumentException exception) {
            assertTrue(exception.getMessage().length()>0);
        }
        RasterFrame frame=RasterFrame.wrap(1, 1, new int[] {0});
        try {
            frame.getPixel(-1, 0);
            fail("Expected negative x to be rejected");
        } catch (IllegalArgumentException exception) {
            assertTrue(exception.getMessage().length()>0);
        }
        try {
            frame.setPixel(0, 1, 0);
            fail("Expected excessive y to be rejected");
        } catch (IllegalArgumentException exception) {
            assertTrue(exception.getMessage().length()>0);
        }
    }
}
