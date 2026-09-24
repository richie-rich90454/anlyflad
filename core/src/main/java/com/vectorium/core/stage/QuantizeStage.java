package com.vectorium.core.stage;
import java.util.Arrays;
import com.vectorium.core.model.StageDescriptor;
import com.vectorium.core.model.VectorDocument;
import com.vectorium.core.raster.PixelQuantizer;
public final class QuantizeStage implements ConfigurableStage {
    private final StageDescriptor descriptor;
    private final int[] palette;
    private final PixelQuantizer quantizer;
    public QuantizeStage(StageDescriptor descriptor, int[] palette) {
        if (descriptor==null) {
            throw new IllegalArgumentException("descriptor must not be null");
        }
        if (palette==null||palette.length==0) {
            throw new IllegalArgumentException("palette must not be null or empty");
        }
        this.descriptor=descriptor;
        this.palette=palette.clone();
        this.quantizer=new PixelQuantizer(palette);
    }
    public Stage withConfig(PipelineConfig config) {
        if (config==null) {
            throw new IllegalArgumentException("config must not be null");
        }
        int[] configured=config.getIntegerArray(descriptor.getName(), "palette", palette);
        if (Arrays.equals(palette, configured)) {
            return this;
        }
        return new QuantizeStage(descriptor, configured);
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
            quantizer.quantize(pixels);
            return document.withPixels(pixels);
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
