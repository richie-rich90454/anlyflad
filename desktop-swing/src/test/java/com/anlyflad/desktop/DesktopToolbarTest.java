package com.anlyflad.desktop;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import com.anlyflad.core.stage.RasterMode;
import com.anlyflad.core.stage.VectorMode;
public final class DesktopToolbarTest {
    @Test
    public void shouldExposePresetAndCleanState() {
        DesktopToolbar toolbar=new DesktopToolbar();
        assertEquals("default", toolbar.getPreset());
        assertEquals(RasterMode.COLOR, toolbar.getRasterMode());
        assertEquals(VectorMode.CURVE, toolbar.getVectorMode());
        assertEquals("balanced", toolbar.getQuality());
        assertTrue(toolbar.isClean());
        toolbar.setPreset("accurate");
        toolbar.setRasterMode(RasterMode.BINARY);
        toolbar.setQuality("max");
        toolbar.setClean(false);
        toolbar.setExportEnabled(true);
        assertEquals("accurate", toolbar.getPreset());
        assertEquals(RasterMode.BINARY, toolbar.getRasterMode());
        assertEquals("max", toolbar.getQuality());
        assertFalse(toolbar.isClean());
    }
}
