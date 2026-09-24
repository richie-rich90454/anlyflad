package com.vectorium.desktop;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
public final class DesktopToolbarTest {
    @Test
    public void shouldExposePresetAndCleanState() {
        DesktopToolbar toolbar=new DesktopToolbar();
        assertEquals("default", toolbar.getPreset());
        assertTrue(toolbar.isClean());
        toolbar.setPreset("accurate");
        toolbar.setClean(false);
        toolbar.setExportEnabled(true);
        assertEquals("accurate", toolbar.getPreset());
        assertFalse(toolbar.isClean());
    }
}
