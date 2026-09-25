package com.anlyflad.core.perf;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
public final class ColorLutTest {
    @Test
    public void shouldReturnRgbAndPreserveSourceAlpha() {
        int[] palette={0x80112233, 0xFF445566};
        ColorLut lut=new ColorLut(palette);
        assertEquals(0x00112233, lut.lookup(0x11, 0x22, 0x33));
        assertEquals(0x00112233, lut.lookup(0x112233));
        assertEquals(0x80112233, lut.lookupArgb(0x80112233));
        assertEquals(0x7F112233, lut.lookupArgb(0x7F112233));
        palette[0]=0xFF000000;
        assertEquals(0x00112233, lut.lookup(0x11, 0x22, 0x33));
        assertEquals(0x80112233, lut.lookupArgb(0x80112233));
    }
    @Test
    public void shouldUseFirstRgbEntryOnAnActualEqualDistanceTie() {
        ColorLut lut=new ColorLut(new int[]{0x000083, 0x000085});
        assertEquals(0x000083, lut.lookup(128, 0, 0));
        assertEquals(0x000083, lut.lookup(0x800000));
    }
    @Test
    public void shouldKeepRgbPaletteEntriesPacked() {
        ColorLut lut=new ColorLut(new int[]{0x102030});
        assertEquals(0x102030, lut.lookup(0x10, 0x20, 0x30));
        assertEquals(0x102030, lut.lookup(0x102030));
        assertEquals(0xAB102030, lut.lookupArgb(0xAB102030));
    }
    @Test
    public void shouldRejectNullAndEmptyPalettes() {
        try {
            new ColorLut(null);
            fail("Expected null palette to be rejected");
        } catch (IllegalArgumentException exception) {
            assertTrue(exception.getMessage().length()>0);
        }
        try {
            new ColorLut(new int[0]);
            fail("Expected empty palette to be rejected");
        } catch (IllegalArgumentException exception) {
            assertTrue(exception.getMessage().length()>0);
        }
    }
}
