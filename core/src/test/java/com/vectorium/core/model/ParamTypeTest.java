package com.vectorium.core.model;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
public final class ParamTypeTest {
    @Test
    public void shouldExposeClosedParameterTypes() {
        assertArrayEquals(new ParamType[]{ParamType.BOOLEAN, ParamType.INTEGER, ParamType.DOUBLE, ParamType.STRING}, ParamType.values());
    }
    @Test
    public void shouldUseStableTypeNames() {
        assertEquals("BOOLEAN", ParamType.BOOLEAN.toString());
        assertEquals("INTEGER", ParamType.INTEGER.toString());
        assertEquals("DOUBLE", ParamType.DOUBLE.toString());
        assertEquals("STRING", ParamType.STRING.toString());
    }
}
