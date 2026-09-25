package com.anlyflad.core.stage;
import java.util.Arrays;
import com.anlyflad.core.model.StageDescriptor;
import com.anlyflad.core.model.VectorDocument;
import com.anlyflad.core.raster.ThresholdService;
public final class ContourStage implements ConfigurableStage, ColorTransformStage {
    private final StageDescriptor descriptor;
    private final int threshold;
    public ContourStage(StageDescriptor descriptor, int threshold) {
        if (descriptor==null) {
            throw new IllegalArgumentException("descriptor must not be null");
        }
        if (threshold<0||threshold>255) {
            throw new IllegalArgumentException("threshold must be between 0 and 255");
        }
        this.descriptor=descriptor;
        this.threshold=threshold;
    }
    public Stage withConfig(PipelineConfig config) {
        if (config==null) {
            throw new IllegalArgumentException("config must not be null");
        }
        int configured=config.getInteger(descriptor.getName(), "threshold", threshold);
        if (configured==threshold) {
            return this;
        }
        return new ContourStage(descriptor, configured);
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
        if (document==null) {
            throw new StageException("document must not be null");
        }
        if (!document.getOrigin().isRaster()) {
            throw new StageException("contour requires a raster document");
        }
        int[] pixels=Arrays.copyOf(document.getOwnedPixels(), document.getOwnedPixels().length);
        try {
            ThresholdService.apply(pixels, threshold);
            return document.withOwnedPixels(pixels);
        } catch (IllegalArgumentException exception) {
            throw new StageException("unable to threshold raster", exception);
        }
    }
}
