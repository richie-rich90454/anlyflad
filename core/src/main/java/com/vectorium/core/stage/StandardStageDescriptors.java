package com.vectorium.core.stage;
import java.util.Arrays;
import java.util.Collections;
import com.vectorium.core.model.ParamSpec;
import com.vectorium.core.model.ParamType;
import com.vectorium.core.model.StageDescriptor;
import com.vectorium.core.raster.AdaptiveColorQuantizer;
public final class StandardStageDescriptors {
    public static final StageDescriptor VALIDATE=new StageDescriptor("validate", "Validate", "Checks document integrity", true);
    public static final StageDescriptor PREPROCESS=new StageDescriptor("preprocess", "Preprocess", "Prepares raster pixels", true, Arrays.<ParamSpec>asList(new ParamSpec("grayscale", "Grayscale", "Convert pixels to grayscale", ParamType.BOOLEAN, Boolean.TRUE, null, null), new ParamSpec("brightness", "Brightness", "Adjust channel brightness", ParamType.INTEGER, Integer.valueOf(0), Double.valueOf(-255.0), Double.valueOf(255.0)), new ParamSpec("contrast", "Contrast", "Adjust channel contrast", ParamType.DOUBLE, Double.valueOf(1.0), Double.valueOf(0.0), Double.valueOf(2.0))));
    public static final StageDescriptor QUANTIZE=new StageDescriptor("quantize", "Quantize", "Maps pixels to a compact palette", true, Arrays.<ParamSpec>asList(new ParamSpec("palette", "Palette", "Comma-separated RGB values", ParamType.STRING, "0,16777215,65280,16711680", null, null), new ParamSpec("maxColors", "Maximum Colors", "Maximum adaptive RGB palette size for contour and curve modes", ParamType.INTEGER, Integer.valueOf(AdaptiveColorQuantizer.DEFAULT_MAX_COLORS), Double.valueOf(1.0), Double.valueOf(AdaptiveColorQuantizer.MAX_COLORS))));
    public static final StageDescriptor CONTOUR=new StageDescriptor("contour", "Contour", "Creates binary foreground contours", true, Arrays.<ParamSpec>asList(new ParamSpec("threshold", "Threshold", "Foreground luminance threshold", ParamType.INTEGER, Integer.valueOf(128), Double.valueOf(0.0), Double.valueOf(255.0))));
    public static final StageDescriptor VECTORIZE=new StageDescriptor("vectorize", "Vectorize", "Converts raster pixels to vector paths", true, Arrays.<ParamSpec>asList(new ParamSpec("mode", "Vector Mode", "Exact runs, region contours, or fitted curves", ParamType.STRING, "curve", null, null), new ParamSpec("maxPaths", "Maximum Paths", "Maximum output path count", ParamType.INTEGER, Integer.valueOf(250000), Double.valueOf(1.0), Double.valueOf(1000000.0)), new ParamSpec("maxVertices", "Maximum Vertices", "Maximum contour vertex count", ParamType.INTEGER, Integer.valueOf(4000000), Double.valueOf(1.0), Double.valueOf(4000000.0)), new ParamSpec("curveTolerance", "Curve Tolerance", "Maximum curve fitting error", ParamType.DOUBLE, Double.valueOf(0.5), Double.valueOf(0.0), Double.valueOf(100.0))));
    public static final StageDescriptor SPECK_FILTER=new StageDescriptor("speck-filter", "Speck Filter", "Removes paths below an area threshold", true, Collections.<ParamSpec>singletonList(new ParamSpec("minArea", "Minimum Area", "Remove smaller path areas", ParamType.DOUBLE, Double.valueOf(1.0), Double.valueOf(0.0), Double.valueOf(1000000.0))));
    public static final StageDescriptor COLOR_MERGE=new StageDescriptor("color-merge", "Color Merge", "Merges nearby similar colors", true, Collections.<ParamSpec>singletonList(new ParamSpec("distance", "Color Distance", "Maximum RGB distance for equal alpha", ParamType.DOUBLE, Double.valueOf(8.0), Double.valueOf(0.0), Double.valueOf(441.0))));
    public static final StageDescriptor UNION=new StageDescriptor("union", "Union", "Unions touching rectangular paths", true, Collections.<ParamSpec>singletonList(new ParamSpec("distance", "Union Distance", "Maximum rectangle gap", ParamType.DOUBLE, Double.valueOf(0.0), Double.valueOf(0.0), Double.valueOf(1000.0))));
    public static final StageDescriptor SIMPLIFY=new StageDescriptor("simplify", "Simplify", "Removes redundant path nodes", true, Collections.<ParamSpec>singletonList(new ParamSpec("tolerance", "Tolerance", "Maximum simplification distance", ParamType.DOUBLE, Double.valueOf(1.0), Double.valueOf(0.0), Double.valueOf(100.0))));
    public static final StageDescriptor SMOOTH=new StageDescriptor("smooth", "Smooth", "Smooths path corners", true, Collections.<ParamSpec>singletonList(new ParamSpec("passes", "Passes", "Number of smoothing passes", ParamType.INTEGER, Integer.valueOf(1), Double.valueOf(0.0), Double.valueOf(3.0))));
    public static final StageDescriptor HOLE_FIX=new StageDescriptor("hole-fix", "Hole Fix", "Removes small enclosed paths", true, Collections.<ParamSpec>singletonList(new ParamSpec("maxArea", "Maximum Hole Area", "Largest removable enclosed area", ParamType.DOUBLE, Double.valueOf(100.0), Double.valueOf(0.0), Double.valueOf(1000000.0))));
    public static final StageDescriptor LAYER_SORT=new StageDescriptor("layer-sort", "Layer Sort", "Sorts paths by area", true, Collections.<ParamSpec>singletonList(new ParamSpec("descending", "Descending", "Sort largest first", ParamType.BOOLEAN, Boolean.TRUE, null, null)));
    public static final StageDescriptor DEDUPE=new StageDescriptor("dedupe", "Deduplicate", "Removes duplicate paths", true, Collections.<ParamSpec>singletonList(new ParamSpec("tolerance", "Tolerance", "Maximum coordinate difference", ParamType.DOUBLE, Double.valueOf(0.1), Double.valueOf(0.0), Double.valueOf(100.0))));
    public static final StageDescriptor NORMALIZE=new StageDescriptor("normalize", "Normalize", "Moves geometry to a nonnegative origin", true, Collections.<ParamSpec>singletonList(new ParamSpec("enabled", "Enabled", "Apply coordinate normalization", ParamType.BOOLEAN, Boolean.TRUE, null, null)));
    public static final StageDescriptor SERIALIZE=new StageDescriptor("serialize", "Serialize", "Warms the optimized SVG cache", true);
    private StandardStageDescriptors() {
    }
    public static StageDescriptor get(String name) {
        if (VALIDATE.getName().equals(name)) {
            return VALIDATE;
        }
        if (PREPROCESS.getName().equals(name)) {
            return PREPROCESS;
        }
        if (QUANTIZE.getName().equals(name)) {
            return QUANTIZE;
        }
        if (CONTOUR.getName().equals(name)) {
            return CONTOUR;
        }
        if (VECTORIZE.getName().equals(name)) {
            return VECTORIZE;
        }
        if (SPECK_FILTER.getName().equals(name)) {
            return SPECK_FILTER;
        }
        if (COLOR_MERGE.getName().equals(name)) {
            return COLOR_MERGE;
        }
        if (UNION.getName().equals(name)) {
            return UNION;
        }
        if (SIMPLIFY.getName().equals(name)) {
            return SIMPLIFY;
        }
        if (SMOOTH.getName().equals(name)) {
            return SMOOTH;
        }
        if (HOLE_FIX.getName().equals(name)) {
            return HOLE_FIX;
        }
        if (LAYER_SORT.getName().equals(name)) {
            return LAYER_SORT;
        }
        if (DEDUPE.getName().equals(name)) {
            return DEDUPE;
        }
        if (NORMALIZE.getName().equals(name)) {
            return NORMALIZE;
        }
        if (SERIALIZE.getName().equals(name)) {
            return SERIALIZE;
        }
        throw new IllegalArgumentException("Unknown stage: "+name);
    }
}
