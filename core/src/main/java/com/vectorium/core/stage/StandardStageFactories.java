package com.vectorium.core.stage;
import com.vectorium.core.raster.AdaptiveColorQuantizer;
import com.vectorium.core.svg.SvgCache;
public final class StandardStageFactories {
    private final SerializeFactory serializeFactory;
    public StandardStageFactories(SvgCache cache) {
        if (cache==null) {
            throw new IllegalArgumentException("cache must not be null");
        }
        this.serializeFactory=new SerializeFactory(cache);
    }
    public ValidateFactory validate() {
        return new ValidateFactory();
    }
    public PreprocessFactory preprocess() {
        return new PreprocessFactory();
    }
    public QuantizeFactory quantize() {
        return new QuantizeFactory();
    }
    public ContourFactory contour() {
        return new ContourFactory();
    }
    public VectorizeFactory vectorize() {
        return new VectorizeFactory();
    }
    public SpeckFilterFactory speckFilter() {
        return new SpeckFilterFactory();
    }
    public ColorMergeFactory colorMerge() {
        return new ColorMergeFactory();
    }
    public UnionFactory union() {
        return new UnionFactory();
    }
    public SimplifyFactory simplify() {
        return new SimplifyFactory();
    }
    public SmoothFactory smooth() {
        return new SmoothFactory();
    }
    public HoleFixFactory holeFix() {
        return new HoleFixFactory();
    }
    public LayerSortFactory layerSort() {
        return new LayerSortFactory();
    }
    public DedupeFactory dedupe() {
        return new DedupeFactory();
    }
    public NormalizeFactory normalize() {
        return new NormalizeFactory();
    }
    public SerializeFactory serialize() {
        return serializeFactory;
    }
    public static final class ValidateFactory implements StageFactory {
        private ValidateFactory() {
        }
        public Stage create() {
            return new ValidateStage(StandardStageDescriptors.VALIDATE);
        }
    }
    public static final class PreprocessFactory implements StageFactory {
        private PreprocessFactory() {
        }
        public Stage create() {
            return new PreprocessStage(StandardStageDescriptors.PREPROCESS, true, 0, 1.0);
        }
    }
    public static final class QuantizeFactory implements StageFactory {
        private QuantizeFactory() {
        }
        public Stage create() {
            return new QuantizeStage(StandardStageDescriptors.QUANTIZE, AdaptiveColorQuantizer.DEFAULT_MAX_COLORS);
        }
    }
    public static final class ContourFactory implements StageFactory {
        private ContourFactory() {
        }
        public Stage create() {
            return new ContourStage(StandardStageDescriptors.CONTOUR, 128);
        }
    }
    public static final class VectorizeFactory implements StageFactory {
        private VectorizeFactory() {
        }
        public Stage create() {
            return new VectorizeStage(StandardStageDescriptors.VECTORIZE);
        }
    }
    public static final class SpeckFilterFactory implements StageFactory {
        private SpeckFilterFactory() {
        }
        public Stage create() {
            return new SpeckFilterStage(StandardStageDescriptors.SPECK_FILTER, 1.0);
        }
    }
    public static final class ColorMergeFactory implements StageFactory {
        private ColorMergeFactory() {
        }
        public Stage create() {
            return new ColorMergeStage(StandardStageDescriptors.COLOR_MERGE, 8.0);
        }
    }
    public static final class UnionFactory implements StageFactory {
        private UnionFactory() {
        }
        public Stage create() {
            return new UnionStage(StandardStageDescriptors.UNION, 0.0);
        }
    }
    public static final class SimplifyFactory implements StageFactory {
        private SimplifyFactory() {
        }
        public Stage create() {
            return new SimplifyStage(StandardStageDescriptors.SIMPLIFY, 1.0);
        }
    }
    public static final class SmoothFactory implements StageFactory {
        private SmoothFactory() {
        }
        public Stage create() {
            return new SmoothStage(StandardStageDescriptors.SMOOTH, 1);
        }
    }
    public static final class HoleFixFactory implements StageFactory {
        private HoleFixFactory() {
        }
        public Stage create() {
            return new HoleFixStage(StandardStageDescriptors.HOLE_FIX, 100.0);
        }
    }
    public static final class LayerSortFactory implements StageFactory {
        private LayerSortFactory() {
        }
        public Stage create() {
            return new LayerSortStage(StandardStageDescriptors.LAYER_SORT, true);
        }
    }
    public static final class DedupeFactory implements StageFactory {
        private DedupeFactory() {
        }
        public Stage create() {
            return new DedupeStage(StandardStageDescriptors.DEDUPE, 0.1);
        }
    }
    public static final class NormalizeFactory implements StageFactory {
        private NormalizeFactory() {
        }
        public Stage create() {
            return new NormalizeStage(StandardStageDescriptors.NORMALIZE, true);
        }
    }
    public static final class SerializeFactory implements StageFactory {
        private final SvgCache cache;
        private SerializeFactory(SvgCache cache) {
            this.cache=cache;
        }
        public Stage create() {
            return new SerializeStage(StandardStageDescriptors.SERIALIZE, cache);
        }
    }
}
