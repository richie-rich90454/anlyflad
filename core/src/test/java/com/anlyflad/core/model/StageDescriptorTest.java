package com.anlyflad.core.model;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
public final class StageDescriptorTest {
    @Test
    public void shouldStoreDescriptorValues() {
        StageDescriptor descriptor=new StageDescriptor("trace", "Trace Edges", "Finds image edges", true);
        assertEquals("trace", descriptor.getName());
        assertEquals("Trace Edges", descriptor.getLabel());
        assertEquals("Finds image edges", descriptor.getDescription());
        assertTrue(descriptor.isDefaultEnabled());
    }
    @Test
    public void shouldRejectInvalidTextValues() {
        String[] invalidValues={null, "", "   "};
        for (String value : invalidValues) {
            try {
                new StageDescriptor(value, "Label", "Description", true);
                fail("Expected invalid name to be rejected");
            } catch (IllegalArgumentException exception) {
                assertEquals(true, exception.getMessage().length()>0);
            }
            try {
                new StageDescriptor("name", value, "Description", true);
                fail("Expected invalid label to be rejected");
            } catch (IllegalArgumentException exception) {
                assertEquals(true, exception.getMessage().length()>0);
            }
            try {
                new StageDescriptor("name", "Label", value, true);
                fail("Expected invalid description to be rejected");
            } catch (IllegalArgumentException exception) {
                assertEquals(true, exception.getMessage().length()>0);
            }
        }
    }
    @Test
    public void shouldStoreImmutableParameterDefinitions() {
        ParamSpec parameter=new ParamSpec("threshold", "Threshold", "Detection threshold", ParamType.DOUBLE, Double.valueOf(128.0), Double.valueOf(0.0), Double.valueOf(255.0));
        List<ParamSpec> source=new ArrayList<ParamSpec>();
        source.add(parameter);
        StageDescriptor descriptor=new StageDescriptor("trace", "Trace Edges", "Finds image edges", true, source);
        source.clear();
        assertEquals(1, descriptor.getParameters().size());
        assertEquals(parameter, descriptor.getParameters().get(0));
        try {
            descriptor.getParameters().clear();
            fail("Expected descriptor parameters to be unmodifiable");
        } catch (UnsupportedOperationException exception) {
            assertEquals(UnsupportedOperationException.class, exception.getClass());
        }
    }
    @Test
    public void shouldUseValueEqualityHashCodeAndString() {
        StageDescriptor first=new StageDescriptor("trace", "Trace Edges", "Finds image edges", true);
        StageDescriptor second=new StageDescriptor("trace", "Trace Edges", "Finds image edges", true);
        assertEquals(first, second);
        assertEquals(first.hashCode(), second.hashCode());
        assertNotEquals(first, new StageDescriptor("trace", "Trace Edges", "Finds image edges", false));
        assertFalse(new StageDescriptor("trace", "Trace Edges", "Finds image edges", false).isDefaultEnabled());
        assertEquals("StageDescriptor{name=trace, label=Trace Edges, description=Finds image edges, defaultEnabled=true}", first.toString());
    }
}
