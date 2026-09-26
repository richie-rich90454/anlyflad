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
    public static final String QUALITY_DRAFT="draft";
    public static final String QUALITY_BALANCED="balanced";
    public static final String QUALITY_MAX="max";
    private static final double DRAFT_TOLERANCE=1.5;
    private static final double MAX_TOLERANCE=0.2;
    private static final int DRAFT_SUPERSAMPLE=1;
    private static final int MAX_SUPERSAMPLE=4;
    private static final double MAX_OUTPUT_SCALE=16.0;
    private final StageDescriptor descriptor;
    private final RasterMode rasterMode;
    private final VectorMode vectorMode;
    private final int maxPaths;
    private final int maxVertices;
    private final double curveTolerance;
    private final String quality;
    private final int supersample;
    private final double outputScale;
    public VectorizeStage(StageDescriptor descriptor) {
        this(descriptor, RasterMode.COLOR, VectorMode.CURVE, ColorRasterVectorizer.DEFAULT_MAX_PATHS, ColorContourVectorizer.DEFAULT_MAX_VERTICES, ColorCurveVectorizer.DEFAULT_TOLERANCE, QUALITY_BALANCED, 0, 1.0);
    }
    public VectorizeStage(StageDescriptor descriptor, RasterMode rasterMode, int maxPaths) {
        this(descriptor, rasterMode, VectorMode.CURVE, maxPaths, ColorContourVectorizer.DEFAULT_MAX_VERTICES, ColorCurveVectorizer.DEFAULT_TOLERANCE, QUALITY_BALANCED, 0, 1.0);
    }
    public VectorizeStage(StageDescriptor descriptor, RasterMode rasterMode, VectorMode vectorMode, int maxPaths, int maxVertices) {
        this(descriptor, rasterMode, vectorMode, maxPaths, maxVertices, ColorCurveVectorizer.DEFAULT_TOLERANCE, QUALITY_BALANCED, 0, 1.0);
    }
    public VectorizeStage(StageDescriptor descriptor, RasterMode rasterMode, VectorMode vectorMode, int maxPaths, int maxVertices, double curveTolerance) {
        this(descriptor, rasterMode, vectorMode, maxPaths, maxVertices, curveTolerance, QUALITY_BALANCED, 0, 1.0);
    }
    public VectorizeStage(StageDescriptor descriptor, RasterMode rasterMode, VectorMode vectorMode, int maxPaths, int maxVertices, double curveTolerance, String quality, int supersample, double outputScale) {
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
        requireQuality(quality);
        if (supersample<0||supersample>MAX_SUPERSAMPLE) {
            throw new IllegalArgumentException("supersample must be between 0 and "+MAX_SUPERSAMPLE);
        }
        if (!Double.isFinite(outputScale)||outputScale<=0.0||outputScale>MAX_OUTPUT_SCALE) {
            throw new IllegalArgumentException("outputScale must be between 0 and "+MAX_OUTPUT_SCALE);
        }
        this.descriptor=descriptor;
        this.rasterMode=rasterMode;
        this.vectorMode=vectorMode;
        this.maxPaths=maxPaths;
        this.maxVertices=maxVertices;
        this.curveTolerance=curveTolerance;
        this.quality=quality;
        this.supersample=supersample;
        this.outputScale=outputScale;
    }
    public Stage withConfig(PipelineConfig config) {
        if (config==null) {
            throw new IllegalArgumentException("config must not be null");
        }
        RasterMode configuredMode=config.getRasterMode();
        String modeValue=config.getString(descriptor.getName(), "mode", config.getVectorMode().getOptionName());
        VectorMode configuredVectorMode=VectorMode.parse(modeValue);
        boolean maxPathsConfigured=config.getString(descriptor.getName(), "maxPaths", null)!=null;
        int configuredMaxPaths=config.getInteger(descriptor.getName(), "maxPaths", maxPaths);
        if (!maxPathsConfigured) {
            configuredMaxPaths=configuredVectorMode==VectorMode.EXACT?ColorRasterVectorizer.DEFAULT_MAX_PATHS:ColorContourVectorizer.DEFAULT_MAX_PATHS;
        } else if (configuredVectorMode!=VectorMode.EXACT&&(configuredMaxPaths<1||configuredMaxPaths>ColorContourVectorizer.MAX_PATHS)) {
            throw new IllegalArgumentException("maxPaths must be between 1 and "+ColorContourVectorizer.MAX_PATHS+" for contour and curve modes");
        }
        int configuredMaxVertices=config.getInteger(descriptor.getName(), "maxVertices", maxVertices);
        boolean qualityConfigured=config.getString(descriptor.getName(), "quality", null)!=null;
        String configuredQuality=config.getString(descriptor.getName(), "quality", quality);
        requireQuality(configuredQuality);
        boolean toleranceConfigured=config.getString(descriptor.getName(), "curveTolerance", null)!=null;
        double configuredCurveTolerance=config.getDouble(descriptor.getName(), "curveTolerance", curveTolerance);
        if (qualityConfigured&&!toleranceConfigured) {
            configuredCurveTolerance=toleranceForQuality(configuredQuality);
        }
        boolean supersampleConfigured=config.getString(descriptor.getName(), "supersample", null)!=null;
        int configuredSupersample=config.getInteger(descriptor.getName(), "supersample", supersample);
        if (qualityConfigured&&!supersampleConfigured) {
            configuredSupersample=supersampleForQuality(configuredQuality);
        }
        if (configuredSupersample<0||configuredSupersample>MAX_SUPERSAMPLE) {
            throw new IllegalArgumentException("supersample must be between 0 and "+MAX_SUPERSAMPLE);
        }
        boolean outputScaleConfigured=config.getString(descriptor.getName(), "outputScale", null)!=null;
        double configuredOutputScale=config.getDouble(descriptor.getName(), "outputScale", outputScale);
        if (outputScaleConfigured&&(!Double.isFinite(configuredOutputScale)||configuredOutputScale<=0.0||configuredOutputScale>MAX_OUTPUT_SCALE)) {
            throw new IllegalArgumentException("outputScale must be between 0 and "+MAX_OUTPUT_SCALE);
        }
        if (configuredMode==rasterMode&&configuredVectorMode==vectorMode&&configuredMaxPaths==maxPaths&&configuredMaxVertices==maxVertices&&Double.doubleToLongBits(configuredCurveTolerance)==Double.doubleToLongBits(curveTolerance)&&configuredQuality.equals(quality)&&configuredSupersample==supersample&&Double.doubleToLongBits(configuredOutputScale)==Double.doubleToLongBits(outputScale)) {
            return this;
        }
        return new VectorizeStage(descriptor, configuredMode, configuredVectorMode, configuredMaxPaths, configuredMaxVertices, configuredCurveTolerance, configuredQuality, configuredSupersample, configuredOutputScale);
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
            VectorDocument output;
            if (vectorMode==VectorMode.EXACT) {
                if (rasterMode==RasterMode.COLOR) {
                    output=document.withPathsAndOwnedPixels(ColorRasterVectorizer.vectorize(frame, maxPaths));
                } else {
                    output=document.withPathsAndOwnedPixels(RasterVectorizer.vectorize(frame, maxPaths));
                }
            } else if (vectorMode==VectorMode.CURVE) {
                if (rasterMode==RasterMode.BINARY) {
                    output=document.withPathsAndOwnedPixels(validateCubicBudget(ColorCurveVectorizer.vectorizeForeground(frame, curveTolerance, maxPaths, maxVertices), maxVertices));
                } else {
                    output=document.withPathsAndOwnedPixels(validateCubicBudget(ColorCurveVectorizer.vectorize(frame, curveTolerance, maxPaths, maxVertices, supersample), maxVertices));
                }
            } else if (rasterMode==RasterMode.BINARY) {
                output=document.withPathsAndOwnedPixels(ColorContourVectorizer.vectorizeForeground(frame, maxPaths, maxVertices));
            } else {
                output=document.withPathsAndOwnedPixels(ColorContourVectorizer.vectorizeSupersampled(frame, maxPaths, maxVertices, supersample));
            }
            return outputScale==1.0?output:output.withOutputScale(outputScale);
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
    private static void requireQuality(String quality) {
        if (!QUALITY_DRAFT.equals(quality)&&!QUALITY_BALANCED.equals(quality)&&!QUALITY_MAX.equals(quality)) {
            throw new IllegalArgumentException("quality must be draft, balanced, or max");
        }
    }
    private static double toleranceForQuality(String quality) {
        if (QUALITY_DRAFT.equals(quality)) {
            return DRAFT_TOLERANCE;
        }
        if (QUALITY_MAX.equals(quality)) {
            return MAX_TOLERANCE;
        }
        return ColorCurveVectorizer.DEFAULT_TOLERANCE;
    }
    private static int supersampleForQuality(String quality) {
        if (QUALITY_DRAFT.equals(quality)) {
            return DRAFT_SUPERSAMPLE;
        }
        if (QUALITY_MAX.equals(quality)) {
            return MAX_SUPERSAMPLE;
        }
        return 0;
    }
}
