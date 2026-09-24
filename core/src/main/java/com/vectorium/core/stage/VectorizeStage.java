package com.vectorium.core.stage;
import com.vectorium.core.model.StageDescriptor;
import com.vectorium.core.model.VectorDocument;
import com.vectorium.core.raster.RasterFrame;
import com.vectorium.core.raster.RasterVectorizer;
public final class VectorizeStage implements ConfigurableStage {
    private final StageDescriptor descriptor;
    public VectorizeStage(StageDescriptor descriptor) {
        if (descriptor==null) {
            throw new IllegalArgumentException("descriptor must not be null");
        }
        this.descriptor=descriptor;
    }
    public Stage withConfig(PipelineConfig config) {
        if (config==null) {
            throw new IllegalArgumentException("config must not be null");
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
        if (document==null) {
            throw new StageException("document must not be null");
        }
        if (!document.getOrigin().isRaster()) {
            throw new StageException("vectorize requires a raster document");
        }
        try {
            RasterFrame frame=RasterFrame.wrap(document.getWidth(), document.getHeight(), document.getOwnedPixels());
            return document.withPaths(RasterVectorizer.vectorize(frame));
        } catch (IllegalArgumentException exception) {
            throw new StageException("unable to vectorize raster", exception);
        }
    }
}
