package com.vectorium.desktop;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import com.vectorium.core.stage.RasterMode;
import com.vectorium.core.stage.VectorMode;
public final class DesktopToolbarTest {
    @Test
    public void shouldExposePresetAndCleanState() {
        DesktopToolbar toolbar=new DesktopToolbar();
        assertEquals("default", toolbar.getPreset());
        assertEquals(RasterMode.COLOR, toolbar.getRasterMode());
        assertEquals(VectorMode.CURVE, toolbar.getVectorMode());
        assertTrue(toolbar.isClean());
        toolbar.setPreset("accurate");
        toolbar.setRasterMode(RasterMode.BINARY);
        toolbar.setClean(false);
        toolbar.setExportEnabled(true);
        assertEquals("accurate", toolbar.getPreset());
        assertEquals(RasterMode.BINARY, toolbar.getRasterMode());
        assertFalse(toolbar.isClean());
    }
}
