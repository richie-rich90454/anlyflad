package com.vectorium.core.stage;
import java.util.Arrays;
import com.vectorium.core.model.StageDescriptor;
import com.vectorium.core.model.VectorDocument;
import com.vectorium.core.raster.AdaptiveColorQuantizer;
import com.vectorium.core.raster.PixelQuantizer;
public final class QuantizeStage implements ConfigurableStage, ColorTransformStage {
    private static final int[] DEFAULT_BINARY_PALETTE={0x000000, 0xFFFFFF, 0x00FF00, 0xFF0000};
    private final StageDescriptor descriptor;
    private final int[] palette;
    private final int maxColors;
    private final boolean adaptive;
    private final PixelQuantizer quantizer;
    public QuantizeStage(StageDescriptor descriptor, int[] palette) {
        this(descriptor, palette, AdaptiveColorQuantizer.DEFAULT_MAX_COLORS, false);
    }
    public QuantizeStage(StageDescriptor descriptor, int maxColors) {
        this(descriptor, DEFAULT_BINARY_PALETTE, maxColors, true);
    }
    private QuantizeStage(StageDescriptor descriptor, int[] palette, int maxColors, boolean adaptive) {
        if (descriptor==null) {
            throw new IllegalArgumentException("descriptor must not be null");
        }
        if (palette==null||palette.length==0) {
            throw new IllegalArgumentException("palette must not be null or empty");
        }
        if (maxColors<=0||maxColors>AdaptiveColorQuantizer.MAX_COLORS) {
            throw new IllegalArgumentException("maxColors must be between 1 and "+AdaptiveColorQuantizer.MAX_COLORS);
        }
        this.descriptor=descriptor;
        this.palette=palette.clone();
        this.maxColors=maxColors;
        this.adaptive=adaptive;
        this.quantizer=new PixelQuantizer(this.palette);
    }
    public Stage withConfig(PipelineConfig config) {
        if (config==null) {
            throw new IllegalArgumentException("config must not be null");
        }
        String configuredPalette=config.getString(descriptor.getName(), "palette", null);
        if (configuredPalette!=null) {
            int[] values=config.getIntegerArray(descriptor.getName(), "palette", palette);
            if (!adaptive&&Arrays.equals(palette, values)) {
                return this;
            }
            return new QuantizeStage(descriptor, values);
        }
        int configuredMaxColors=config.getInteger(descriptor.getName(), "maxColors", maxColors);
        boolean maxColorsConfigured=config.getString(descriptor.getName(), "maxColors", null)!=null;
        String mode=config.getString("vectorize", "mode", config.getVectorMode().getOptionName());
        boolean useAdaptive=VectorMode.parse(mode)!=VectorMode.EXACT&&(maxColorsConfigured||adaptive&&config.getRasterMode()==RasterMode.COLOR);
        if (useAdaptive) {
            if (adaptive&&configuredMaxColors==maxColors) {
                return this;
            }
            return new QuantizeStage(descriptor, configuredMaxColors);
        }
        if (adaptive) {
            return new QuantizeStage(descriptor, palette);
        }
        return this;
    }
    public String getName() {
        return descriptor.getName();
    }
    public String getLabel() {
        return descriptor.getLabel();
    }
    public String getDescription() {
        return descriptor.getDescription();
    }
    public StageTag getTag() {
        return StageTag.RASTER_ONLY;
    }
    public boolean appliesTo(VectorDocument document) {
        return document!=null&&document.getOrigin().isRaster();
    }
    public VectorDocument apply(VectorDocument document) throws StageException {
        requireRaster(document);
        int[] pixels=Arrays.copyOf(document.getOwnedPixels(), document.getOwnedPixels().length);
        try {
            if (adaptive) {
                new AdaptiveColorQuantizer(maxColors).quantize(pixels);
            } else {
                quantizer.quantize(pixels);
            }
            return document.withOwnedPixels(pixels);
        } catch (IllegalArgumentException exception) {
            throw new StageException("unable to quantize raster", exception);
        }
    }
    private void requireRaster(VectorDocument document) throws StageException {
        if (document==null) {
            throw new StageException("document must not be null");
        }
        if (!document.getOrigin().isRaster()) {
            throw new StageException("quantize requires a raster document");
        }
    }
}
