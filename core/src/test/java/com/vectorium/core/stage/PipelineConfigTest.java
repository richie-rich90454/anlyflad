package com.vectorium.core.stage;
import java.util.Collections;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
public final class PipelineConfigTest {
    @Test
    public void shouldParseTypedOverridesAndBuildImmutableCopies() {
        PipelineConfig defaults=PipelineConfig.defaults();
        assertTrue(defaults.isClean());
        assertEquals("default", defaults.getPresetName());
        assertEquals(RasterMode.COLOR, defaults.getRasterMode());
        assertEquals(VectorMode.CONTOUR, defaults.getVectorMode());
        assertTrue(defaults.getOverrides().isEmpty());
        int[] defaultPalette={0, 0xFFFFFF};
        int[] copiedDefault=defaults.getIntegerArray("quantize", "palette", defaultPalette);
        copiedDefault[0]=1;
        assertEquals(0, defaultPalette[0]);
        PipelineConfig configured=defaults.withClean(false).withPreset("accurate").withStageValue("simplify", "tolerance", "1.5").withStageValue("quantize", "palette", "0,16777215,65280").withStageDisabled("smooth");
        assertFalse(configured.isClean());
        assertEquals("accurate", configured.getPresetName());
        assertEquals(1.5, configured.getDouble("simplify", "tolerance", 0.0), 0.0);
        assertArrayEquals(new int[]{0, 16777215, 65280}, configured.getIntegerArray("quantize", "palette", new int[0]));
        assertFalse(configured.isStageEnabled("smooth"));
        assertTrue(configured.isStageEnabled("simplify"));
        try {
            configured.getOverrides().clear();
            fail("Expected overrides to be unmodifiable");
        } catch (UnsupportedOperationException exception) {
            assertEquals(UnsupportedOperationException.class, exception.getClass());
        }
    }
    @Test
    public void shouldRemoveOnlyTheRequestedStageOverrides() {
        PipelineConfig config=PipelineConfig.defaults().withStageValue("simplify", "tolerance", "2").withStageValue("smooth", "passes", "3").withoutStage("simplify");
        assertFalse(config.getOverrides().containsKey("simplify.tolerance"));
        assertTrue(config.getOverrides().containsKey("smooth.passes"));
        assertEquals(3, config.getInteger("smooth", "passes", 0));
    }
    @Test
    public void shouldUseStableEqualityAndHashCode() {
        PipelineConfig first=PipelineConfig.defaults().withStageValue("simplify", "tolerance", "2");
        PipelineConfig second=PipelineConfig.defaults().withStageValue("simplify", "tolerance", "2");
        assertEquals(first, second);
        assertEquals(first.hashCode(), second.hashCode());
        assertEquals(first.getConfigHash(), second.getConfigHash());
        assertNotEquals(first, second.withStageValue("simplify", "tolerance", "3"));
        assertNotEquals(first, second.withRasterMode(RasterMode.BINARY));
        assertTrue(first.toString().contains("simplify.tolerance=2"));
    }
    @Test
    public void shouldParseRasterModes() {
        assertEquals(RasterMode.COLOR, RasterMode.parse(" COLOR "));
        assertEquals(RasterMode.BINARY, RasterMode.parse("monochrome"));
        try {
            RasterMode.parse("unknown");
            fail("Expected invalid raster mode to be rejected");
        } catch (IllegalArgumentException exception) {
            assertTrue(exception.getMessage().length()>0);
        }
    }
    @Test
    public void shouldParseVectorModes() {
        assertEquals(VectorMode.EXACT, VectorMode.parse(" EXACT "));
        assertEquals(VectorMode.CONTOUR, VectorMode.parse("region"));
        assertEquals(VectorMode.CURVE, VectorMode.parse("fitted"));
        try {
            VectorMode.parse("unknown");
            fail("Expected invalid vector mode to be rejected");
        } catch (IllegalArgumentException exception) {
            assertTrue(exception.getMessage().length()>0);
        }
    }
    @Test
    public void shouldRejectInvalidOverridesAndValues() {
        try {
            PipelineConfig.defaults().withStageValue("bad.key", "value", "1");
            fail("Expected invalid dotted stage name to be rejected");
        } catch (IllegalArgumentException exception) {
            assertTrue(exception.getMessage().length()>0);
        }
        try {
            PipelineConfig.defaults().withStageValue("simplify", "tolerance", "wrong").getInteger("simplify", "tolerance", 0);
            fail("Expected invalid integer to be rejected");
        } catch (IllegalArgumentException exception) {
            assertTrue(exception.getMessage().length()>0);
        }
        try {
            PipelineConfig.defaults().withStageValue("simplify", "tolerance", "NaN").getDouble("simplify", "tolerance", 0.0);
            fail("Expected non-finite value to be rejected");
        } catch (IllegalArgumentException exception) {
            assertTrue(exception.getMessage().length()>0);
        }
        try {
            new PipelineConfig(true, "default", Collections.<String, String>singletonMap("bad", "1"));
            fail("Expected malformed override key to be rejected");
        } catch (IllegalArgumentException exception) {
            assertTrue(exception.getMessage().length()>0);
        }
    }
}
