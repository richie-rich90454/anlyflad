package com.vectorium.core.stage;
import com.vectorium.core.model.StageDescriptor;
import com.vectorium.core.model.VectorDocument;
import com.vectorium.core.raster.ColorRasterVectorizer;
import com.vectorium.core.raster.RasterFrame;
import com.vectorium.core.raster.RasterVectorizer;
public final class VectorizeStage implements ConfigurableStage {
    private final StageDescriptor descriptor;
    private final RasterMode rasterMode;
    private final int maxPaths;
    public VectorizeStage(StageDescriptor descriptor) {
        this(descriptor, RasterMode.COLOR, ColorRasterVectorizer.DEFAULT_MAX_PATHS);
    }
    public VectorizeStage(StageDescriptor descriptor, RasterMode rasterMode, int maxPaths) {
        if (descriptor==null) {
            throw new IllegalArgumentException("descriptor must not be null");
        }
        if (rasterMode==null) {
            throw new IllegalArgumentException("rasterMode must not be null");
        }
        if (maxPaths<=0||maxPaths>ColorRasterVectorizer.MAX_PATHS) {
            throw new IllegalArgumentException("maxPaths must be between 1 and "+ColorRasterVectorizer.MAX_PATHS);
        }
        this.descriptor=descriptor;
        this.rasterMode=rasterMode;
        this.maxPaths=maxPaths;
    }
    public Stage withConfig(PipelineConfig config) {
        if (config==null) {
            throw new IllegalArgumentException("config must not be null");
        }
        RasterMode configuredMode=config.getRasterMode();
        int configuredMaxPaths=config.getInteger(descriptor.getName(), "maxPaths", maxPaths);
        if (configuredMode==rasterMode&&configuredMaxPaths==maxPaths) {
            return this;
        }
        return new VectorizeStage(descriptor, configuredMode, configuredMaxPaths);
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
            if (rasterMode==RasterMode.COLOR) {
                return document.withPaths(ColorRasterVectorizer.vectorize(frame, maxPaths));
            }
            return document.withPaths(RasterVectorizer.vectorize(frame, maxPaths));
        } catch (IllegalArgumentException exception) {
            String message=exception.getMessage();
            if (message==null||message.trim().isEmpty()) {
                message="unable to vectorize raster";
            }
            if (message.indexOf("path limit")>=0) {
                throw StageException.userError(message, exception);
            }
            throw new StageException(message, exception);
        }
    }
}
