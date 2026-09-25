package com.anlyflad.core.svg;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
public final class SvgParseExceptionTest {
    @Test
    public void shouldRetainUsefulMessageAndCause() {
        Throwable cause=new IllegalArgumentException("cause");
        SvgParseException exception=new SvgParseException("useful message", cause);
        assertEquals("useful message", exception.getMessage());
        assertSame(cause, exception.getCause());
        assertTrue(exception instanceof Exception);
    }
    private static void assertSame(Object expected, Object actual) {
        if (expected!=actual) {
            throw new AssertionError("expected same reference");
        }
    }
}
