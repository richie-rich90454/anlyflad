package com.anlyflad.core.model;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
public final class OriginTest {
    @Test
    public void shouldCreateValidatedRasterOrigin() {
        RasterOrigin origin=new RasterOrigin("scan.png");
        assertEquals("scan.png", origin.getSourceName());
        assertTrue(origin.isRaster());
        assertEquals("RasterOrigin{sourceName=scan.png}", origin.toString());
    }
    @Test
    public void shouldCreateValidatedSvgOrigin() {
        SvgOrigin origin=new SvgOrigin("drawing.svg");
        assertEquals("drawing.svg", origin.getSourceName());
        assertFalse(origin.isRaster());
        assertEquals("SvgOrigin{sourceName=drawing.svg}", origin.toString());
    }
    @Test
    public void shouldRejectBlankSourceNames() {
        String[] invalidNames={null, "", "   "};
        for (String sourceName : invalidNames) {
            try {
                new RasterOrigin(sourceName);
                fail("Expected invalid raster source name to be rejected");
            } catch (IllegalArgumentException exception) {
                assertEquals(true, exception.getMessage().length()>0);
            }
            try {
                new SvgOrigin(sourceName);
                fail("Expected invalid SVG source name to be rejected");
            } catch (IllegalArgumentException exception) {
                assertEquals(true, exception.getMessage().length()>0);
            }
        }
    }
    @Test
    public void shouldUseValueEqualityHashCodeAndString() {
        RasterOrigin first=new RasterOrigin("scan.png");
        RasterOrigin second=new RasterOrigin("scan.png");
        SvgOrigin svg=new SvgOrigin("scan.png");
        assertEquals(first, second);
        assertEquals(first.hashCode(), second.hashCode());
        assertNotEquals(first, svg);
        assertEquals("SvgOrigin{sourceName=scan.png}", svg.toString());
    }
}
