package com.vectorium.desktop;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
public final class StatusBarTest {
    @Test
    public void shouldExposeStatusAndProgress() {
        StatusBar status=new StatusBar();
        status.setStatus("Vectorizing");
        status.setProgress(3, 15);
        assertEquals("Vectorizing", status.getStatusText());
        assertEquals(3, status.getProgressValue());
        assertEquals(15, status.getProgressMaximum());
        assertTrue(status.isBusy());
        status.setBusy(false);
        assertFalse(status.isBusy());
    }
}
