package com.anlyflad.core.stage;
import com.anlyflad.core.model.StageDescriptor;
import com.anlyflad.core.model.VectorDocument;
import com.anlyflad.core.model.VectorPath;
import com.anlyflad.core.raster.ColorContourVectorizer;
import com.anlyflad.core.raster.ColorCurveVectorizer;
import com.anlyflad.core.raster.ColorRasterVectorizer;
import com.anlyflad.core.raster.RasterFrame;
import com.anlyflad.core.raster.RasterVectorizer;
public final class VectorizeStage implements ConfigurableStage {
    private final StageDescriptor descriptor;
    private final RasterMode rasterMode;
    private final VectorMode vectorMode;
    private final int maxPaths;
    private final int maxVertices;
    private final double curveTolerance;
    public VectorizeStage(StageDescriptor descriptor) {
        this(descriptor, RasterMode.COLOR, VectorMode.CURVE, ColorRasterVectorizer.DEFAULT_MAX_PATHS, ColorContourVectorizer.DEFAULT_MAX_VERTICES, ColorCurveVectorizer.DEFAULT_TOLERANCE);
    }
    public VectorizeStage(StageDescriptor descriptor, RasterMode rasterMode, int maxPaths) {
        this(descriptor, rasterMode, VectorMode.CURVE, maxPaths, ColorContourVectorizer.DEFAULT_MAX_VERTICES, ColorCurveVectorizer.DEFAULT_TOLERANCE);
    }
    public VectorizeStage(StageDescriptor descriptor, RasterMode rasterMode, VectorMode vectorMode, int maxPaths, int maxVertices) {
        this(descriptor, rasterMode, vectorMode, maxPaths, maxVertices, ColorCurveVectorizer.DEFAULT_TOLERANCE);
    }
    public VectorizeStage(StageDescriptor descriptor, RasterMode rasterMode, VectorMode vectorMode, int maxPaths, int maxVertices, double curveTolerance) {
        if (descriptor==null) {
            throw new IllegalArgumentException("descriptor must not be null");
        }
        if (rasterMode==null) {
            throw new IllegalArgumentException("rasterMode must not be null");
        }
        if (vectorMode==null) {
            throw new IllegalArgumentException("vectorMode must not be null");
        }
        if (maxPaths<=0||maxPaths>ColorRasterVectorizer.MAX_PATHS) {
            throw new IllegalArgumentException("maxPaths must be between 1 and "+ColorRasterVectorizer.MAX_PATHS);
        }
        if (maxVertices<=0||maxVertices>ColorContourVectorizer.MAX_VERTICES) {
            throw new IllegalArgumentException("maxVertices must be between 1 and "+ColorContourVectorizer.MAX_VERTICES);
        }
        if (!Double.isFinite(curveTolerance)||curveTolerance<0.0) {
            throw new IllegalArgumentException("curveTolerance must be finite and nonnegative");
        }
        this.descriptor=descriptor;
        this.rasterMode=rasterMode;
        this.vectorMode=vectorMode;
        this.maxPaths=maxPaths;
        this.maxVertices=maxVertices;
        this.curveTolerance=curveTolerance;
    }
    public Stage withConfig(PipelineConfig config) {
        if (config==null) {
            throw new IllegalArgumentException("config must not be null");
        }
        RasterMode configuredMode=config.getRasterMode();
        String modeValue=config.getString(descriptor.getName(), "mode", config.getVectorMode().getOptionName());
        VectorMode configuredVectorMode=VectorMode.parse(modeValue);
        int configuredMaxPaths=config.getInteger(descriptor.getName(), "maxPaths", maxPaths);
        int configuredMaxVertices=config.getInteger(descriptor.getName(), "maxVertices", maxVertices);
        double configuredCurveTolerance=config.getDouble(descriptor.getName(), "curveTolerance", curveTolerance);
        if (configuredMode==rasterMode&&configuredVectorMode==vectorMode&&configuredMaxPaths==maxPaths&&configuredMaxVertices==maxVertices&&Double.doubleToLongBits(configuredCurveTolerance)==Double.doubleToLongBits(curveTolerance)) {
            return this;
        }
        return new VectorizeStage(descriptor, configuredMode, configuredVectorMode, configuredMaxPaths, configuredMaxVertices, configuredCurveTolerance);
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
            if (vectorMode==VectorMode.EXACT) {
                if (rasterMode==RasterMode.COLOR) {
                    return document.withPathsAndOwnedPixels(validateVertexBudget(ColorRasterVectorizer.vectorize(frame, maxPaths), maxVertices));
                }
                return document.withPathsAndOwnedPixels(validateVertexBudget(RasterVectorizer.vectorize(frame, maxPaths), maxVertices));
            }
            if (vectorMode==VectorMode.CURVE) {
                if (rasterMode==RasterMode.BINARY) {
                    return document.withPathsAndOwnedPixels(validateCubicBudget(ColorCurveVectorizer.vectorizeForeground(frame, curveTolerance, maxPaths, maxVertices), maxVertices));
                }
                return document.withPathsAndOwnedPixels(validateCubicBudget(ColorCurveVectorizer.vectorize(frame, curveTolerance, maxPaths, maxVertices), maxVertices));
            }
            if (rasterMode==RasterMode.BINARY) {
                return document.withPathsAndOwnedPixels(ColorContourVectorizer.vectorizeForeground(frame, maxPaths, maxVertices));
            }
            return document.withPathsAndOwnedPixels(ColorContourVectorizer.vectorize(frame, maxPaths, maxVertices));
        } catch (IllegalArgumentException exception) {
            String message=exception.getMessage();
            if (message==null||message.trim().isEmpty()) {
                message="unable to vectorize raster";
            }
            if (exception instanceof ColorContourVectorizer.ResourceLimitException||message.indexOf("path limit")>=0||message.indexOf("vertex budget")>=0) {
                throw StageException.userError(message, exception);
            }
            throw new StageException(message, exception);
        }
    }
    private java.util.List<VectorPath> validateCubicBudget(java.util.List<VectorPath> paths, int limit) {
        long total=0L;
        for (int index=0;index<paths.size();index++) {
            total+=paths.get(index).getCubicSegmentCount();
            if (total>limit) {
                throw new IllegalArgumentException("cubic segment budget of "+limit+" exceeded; increase maxVertices or simplify the curve tolerance");
            }
        }
        return paths;
    }
    private java.util.List<VectorPath> validateVertexBudget(java.util.List<VectorPath> paths, int limit) {
        long total=0L;
        for (int index=0;index<paths.size();index++) {
            total+=paths.get(index).getNodeCount();
            if (total>limit) {
                throw new IllegalArgumentException("vertex budget of "+limit+" exceeded; increase maxVertices or reduce the raster complexity (required at least "+total+")");
            }
        }
        return paths;
    }
}
