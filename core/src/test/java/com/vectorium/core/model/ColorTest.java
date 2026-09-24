package com.vectorium.core.model;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.fail;
public final class ColorTest {
    @Test
    public void shouldStoreChannelsAndPackArgb() {
        Color color=new Color(12, 34, 56, 78);
        assertEquals(12, color.getRed());
        assertEquals(34, color.getGreen());
        assertEquals(56, color.getBlue());
        assertEquals(78, color.getAlpha());
        assertEquals(0x4E0C2238, color.toArgb());
    }
    @Test
    public void shouldCreateColorFromPackedArgb() {
        Color color=Color.fromArgb(0x89ABCDEF);
        assertEquals(0x89, color.getAlpha());
        assertEquals(0xAB, color.getRed());
        assertEquals(0xCD, color.getGreen());
        assertEquals(0xEF, color.getBlue());
    }
    @Test
    public void shouldParseShortFullAndAlphaHex() {
        assertEquals(new Color(170, 187, 204, 255), Color.fromHex("#abc"));
        assertEquals(new Color(170, 187, 204, 221), Color.fromHex("#abcd"));
        assertEquals(new Color(17, 34, 51, 255), Color.fromHex("#112233"));
        assertEquals(new Color(17, 34, 51, 68), Color.fromHex("#11223344"));
        assertEquals(new Color(170, 187, 204, 221), Color.fromHex("#aBcD"));
    }
    @Test
    public void shouldRejectInvalidHex() {
        String[] invalidHexes={null, "", "abc", "#12", "#gggggg", "#123456789"};
        for (String hex : invalidHexes) {
            try {
                Color.fromHex(hex);
                fail("Expected invalid hex to be rejected: "+hex);
            } catch (IllegalArgumentException exception) {
                assertEquals(true, exception.getMessage().length()>0);
            }
        }
    }
    @Test
    public void shouldRejectChannelsOutsideByteRange() {
        int[] invalidChannels={-1, 256};
        for (int channel : invalidChannels) {
            try {
                new Color(channel, 0, 0, 255);
                fail("Expected invalid red channel to be rejected");
            } catch (IllegalArgumentException exception) {
                assertEquals(true, exception.getMessage().length()>0);
            }
            try {
                new Color(0, channel, 0, 255);
                fail("Expected invalid green channel to be rejected");
            } catch (IllegalArgumentException exception) {
                assertEquals(true, exception.getMessage().length()>0);
            }
            try {
                new Color(0, 0, channel, 255);
                fail("Expected invalid blue channel to be rejected");
            } catch (IllegalArgumentException exception) {
                assertEquals(true, exception.getMessage().length()>0);
            }
            try {
                new Color(0, 0, 0, channel);
                fail("Expected invalid alpha channel to be rejected");
            } catch (IllegalArgumentException exception) {
                assertEquals(true, exception.getMessage().length()>0);
            }
        }
    }
    @Test
    public void shouldUseValueEqualityHashCodeAndString() {
        Color first=new Color(1, 2, 3, 4);
        Color second=new Color(1, 2, 3, 4);
        assertEquals(first, second);
        assertEquals(first.hashCode(), second.hashCode());
        assertNotEquals(first, new Color(1, 2, 3, 5));
        assertEquals("Color{red=1, green=2, blue=3, alpha=4}", first.toString());
    }
}
