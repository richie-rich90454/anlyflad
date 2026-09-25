package com.anlyflad.cli;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import com.anlyflad.core.stage.PipelineConfig;
import com.anlyflad.core.stage.RasterMode;
import com.anlyflad.core.stage.VectorMode;
public final class CliConfigurationTest {
    @Test
    public void shouldApplyPresetAndExplicitValues() {
        List<String> values=Arrays.asList("smooth.passes=2", "simplify.tolerance=0.25");
        List<String> disabled=Collections.singletonList("layer-sort");
        PipelineConfig config=CliConfiguration.create(false, "accurate", values, disabled);
        assertEquals("accurate", config.getPresetName());
        assertFalse(config.isClean());
        assertEquals(2, config.getInteger("smooth", "passes", -1));
        assertEquals(0.25, config.getDouble("simplify", "tolerance", -1.0), 0.0);
        assertFalse(config.isStageEnabled("layer-sort"));
    }
    @Test
    public void shouldAcceptDefaultPresetAndNoOverrides() {
        PipelineConfig config=CliConfiguration.create(true, "default", Collections.<String>emptyList(), Collections.<String>emptyList());
        assertTrue(config.isClean());
        assertEquals(RasterMode.COLOR, config.getRasterMode());
        assertEquals(VectorMode.CURVE, config.getVectorMode());
        assertTrue(config.getOverrides().isEmpty());
        PipelineConfig binary=CliConfiguration.create(true, "default", RasterMode.BINARY, Collections.<String>emptyList(), Collections.<String>emptyList());
        assertEquals(RasterMode.BINARY, binary.getRasterMode());
    }
    @Test
    public void shouldRejectInvalidConfigurationAtTrustBoundary() {
        String[][] invalid=new String[][]{{"unknown", "smooth.passes=1"}, {"default", "smooth=1"}, {"default", "unknown.passes=1"}, {"default", "smooth.unknown=1"}, {"default", "smooth.passes=nope"}, {"default", "smooth.passes=4"}, {"default", "smooth.passes=0.5"}};
        for (int index=0;index<invalid.length;index++) {
            try {
                CliConfiguration.create(true, invalid[index][0], Collections.singletonList(invalid[index][1]), Collections.<String>emptyList());
                fail("Expected invalid configuration to be rejected");
            } catch (IllegalArgumentException exception) {
                assertTrue(exception.getMessage().length()>0);
            }
        }
        String[] disabled=new String[]{"unknown", "validate", "serialize"};
        for (int index=0;index<disabled.length;index++) {
            try {
                CliConfiguration.create(true, "default", Collections.<String>emptyList(), Collections.singletonList(disabled[index]));
                fail("Expected invalid disabled stage to be rejected");
            } catch (IllegalArgumentException exception) {
                assertTrue(exception.getMessage().length()>0);
            }
        }
    }
}
