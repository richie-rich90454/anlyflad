package com.anlyflad.core.model;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.fail;
public final class ParamSpecTest {
    @Test
    public void shouldStoreTypedDefaultWithoutRange() {
        ParamSpec spec=new ParamSpec("enabled", "Enabled", "Whether processing is enabled", ParamType.BOOLEAN, Boolean.TRUE, null, null);
        assertEquals("enabled", spec.getName());
        assertEquals("Enabled", spec.getLabel());
        assertEquals("Whether processing is enabled", spec.getDescription());
        assertEquals(ParamType.BOOLEAN, spec.getType());
        assertEquals(Boolean.TRUE, spec.getDefaultValue());
        assertNull(spec.getMin());
        assertNull(spec.getMax());
    }
    @Test
    public void shouldValidateNumericDefaultsAgainstRange() {
        ParamSpec integerSpec=new ParamSpec("count", "Count", "Number of passes", ParamType.INTEGER, Integer.valueOf(2), Double.valueOf(1.0), Double.valueOf(3.0));
        ParamSpec doubleSpec=new ParamSpec("ratio", "Ratio", "Blend ratio", ParamType.DOUBLE, Double.valueOf(0.5), Double.valueOf(0.0), Double.valueOf(1.0));
        assertEquals(Integer.valueOf(2), integerSpec.getDefaultValue());
        assertEquals(Double.valueOf(1.0), integerSpec.getMin());
        assertEquals(Double.valueOf(3.0), integerSpec.getMax());
        assertEquals(Double.valueOf(0.5), doubleSpec.getDefaultValue());
    }
    @Test
    public void shouldAllowOpenNumericRanges() {
        ParamSpec spec=new ParamSpec("threshold", "Threshold", "Detection threshold", ParamType.DOUBLE, Double.valueOf(10.0), null, Double.valueOf(20.0));
        assertNull(spec.getMin());
        assertEquals(Double.valueOf(20.0), spec.getMax());
    }
    @Test
    public void shouldRejectInvalidMetadata() {
        try {
            new ParamSpec(" ", "Label", "Description", ParamType.STRING, "value", null, null);
            fail("Expected blank name to be rejected");
        } catch (IllegalArgumentException exception) {
            assertEquals(true, exception.getMessage().length()>0);
        }
        try {
            new ParamSpec("name", "", "Description", ParamType.STRING, "value", null, null);
            fail("Expected blank label to be rejected");
        } catch (IllegalArgumentException exception) {
            assertEquals(true, exception.getMessage().length()>0);
        }
        try {
            new ParamSpec("name", "Label", " ", ParamType.STRING, "value", null, null);
            fail("Expected blank description to be rejected");
        } catch (IllegalArgumentException exception) {
            assertEquals(true, exception.getMessage().length()>0);
        }
    }
    @Test
    public void shouldRejectInvalidTypeAndRange() {
        try {
            new ParamSpec("name", "Name", "Description", null, "value", null, null);
            fail("Expected null type to be rejected");
        } catch (IllegalArgumentException exception) {
            assertEquals(true, exception.getMessage().length()>0);
        }
        try {
            new ParamSpec("flag", "Flag", "Description", ParamType.BOOLEAN, "true", null, null);
            fail("Expected mismatched boolean default to be rejected");
        } catch (IllegalArgumentException exception) {
            assertEquals(true, exception.getMessage().length()>0);
        }
        try {
            new ParamSpec("name", "Name", "Description", ParamType.STRING, "value", Double.valueOf(0.0), null);
            fail("Expected range on string parameter to be rejected");
        } catch (IllegalArgumentException exception) {
            assertEquals(true, exception.getMessage().length()>0);
        }
        try {
            new ParamSpec("count", "Count", "Description", ParamType.INTEGER, Integer.valueOf(2), Double.valueOf(3.0), Double.valueOf(1.0));
            fail("Expected reversed range to be rejected");
        } catch (IllegalArgumentException exception) {
            assertEquals(true, exception.getMessage().length()>0);
        }
        try {
            new ParamSpec("count", "Count", "Description", ParamType.INTEGER, Integer.valueOf(4), Double.valueOf(1.0), Double.valueOf(3.0));
            fail("Expected default outside range to be rejected");
        } catch (IllegalArgumentException exception) {
            assertEquals(true, exception.getMessage().length()>0);
        }
        try {
            new ParamSpec("ratio", "Ratio", "Description", ParamType.DOUBLE, Double.NaN, null, null);
            fail("Expected non-finite double default to be rejected");
        } catch (IllegalArgumentException exception) {
            assertEquals(true, exception.getMessage().length()>0);
        }
    }
    @Test
    public void shouldUseValueEqualityHashCodeAndString() {
        ParamSpec first=new ParamSpec("count", "Count", "Pass count", ParamType.INTEGER, Integer.valueOf(2), Double.valueOf(0.0), Double.valueOf(5.0));
        ParamSpec second=new ParamSpec("count", "Count", "Pass count", ParamType.INTEGER, Integer.valueOf(2), Double.valueOf(0.0), Double.valueOf(5.0));
        assertEquals(first, second);
        assertEquals(first.hashCode(), second.hashCode());
        assertNotEquals(first, new ParamSpec("count", "Count", "Pass count", ParamType.INTEGER, Integer.valueOf(3), Double.valueOf(0.0), Double.valueOf(5.0)));
        assertEquals("ParamSpec{name=count, label=Count, description=Pass count, type=INTEGER, defaultValue=2, min=0.0, max=5.0}", first.toString());
    }
}
