package com.vectorium.core.stage;
import java.util.Arrays;
import com.vectorium.core.model.StageDescriptor;
import com.vectorium.core.model.VectorDocument;
import com.vectorium.core.raster.RasterPreprocessor;
public final class PreprocessStage implements ConfigurableStage {
    private final StageDescriptor descriptor;
    private final boolean grayscale;
    private final int brightness;
    private final double contrast;
    public PreprocessStage(StageDescriptor descriptor, boolean grayscale, int brightness, double contrast) {
        if (descriptor==null) {
            throw new IllegalArgumentException("descriptor must not be null");
        }
        this.descriptor=descriptor;
        this.grayscale=grayscale;
        this.brightness=brightness;
        this.contrast=contrast;
        validate(brightness, contrast);
    }
    public Stage withConfig(PipelineConfig config) {
        if (config==null) {
            throw new IllegalArgumentException("config must not be null");
        }
        boolean configuredGrayscale=config.getBoolean(descriptor.getName(), "grayscale", grayscale);
        int configuredBrightness=config.getInteger(descriptor.getName(), "brightness", brightness);
        double configuredContrast=config.getDouble(descriptor.getName(), "contrast", contrast);
        if (configuredGrayscale==grayscale&&configuredBrightness==brightness&&Double.doubleToLongBits(configuredContrast)==Double.doubleToLongBits(contrast)) {
            return this;
        }
        return new PreprocessStage(descriptor, configuredGrayscale, configuredBrightness, configuredContrast);
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
            if (grayscale) {
                RasterPreprocessor.grayscale(pixels);
            }
            if (brightness!=0) {
                RasterPreprocessor.brightness(pixels, brightness);
            }
            if (contrast!=1.0) {
                RasterPreprocessor.contrast(pixels, contrast);
            }
            return document.withPixels(pixels);
        } catch (IllegalArgumentException exception) {
            throw new StageException("unable to preprocess raster", exception);
        }
    }
    private void requireRaster(VectorDocument document) throws StageException {
        if (document==null) {
            throw new StageException("document must not be null");
        }
        if (!document.getOrigin().isRaster()) {
            throw new StageException("preprocess requires a raster document");
        }
    }
    private static void validate(int brightness, double contrast) {
        if (brightness<-255||brightness>255) {
            throw new IllegalArgumentException("brightness must be between -255 and 255");
        }
        if (!Double.isFinite(contrast)||contrast<0.0||contrast>2.0) {
            throw new IllegalArgumentException("contrast must be finite and between zero and two");
        }
    }
}
