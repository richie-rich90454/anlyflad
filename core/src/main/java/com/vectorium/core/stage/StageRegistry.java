package com.vectorium.core.stage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import com.vectorium.core.model.StageDescriptor;
import com.vectorium.core.svg.SvgCache;
public final class StageRegistry {
    private final Map<String, StageFactory> factories;
    private final List<String> names;
    private final PipelineLogger logger;
    private final PipelineMemoizer memoizer;
    public StageRegistry() {
        this(new SilentPipelineLogger(), new BoundedPipelineMemoizer(), new SvgCache());
    }
    public StageRegistry(PipelineLogger logger, PipelineMemoizer memoizer, SvgCache cache) {
        if (logger==null||memoizer==null||cache==null) {
            throw new IllegalArgumentException("registry collaborators must not be null");
        }
        this.logger=logger;
        this.memoizer=memoizer;
        StandardStageFactories stages=new StandardStageFactories(cache);
        LinkedHashMap<String, StageFactory> linkedFactories=new LinkedHashMap<String, StageFactory>();
        linkedFactories.put(StandardStageDescriptors.VALIDATE.getName(), stages.validate());
        linkedFactories.put(StandardStageDescriptors.PREPROCESS.getName(), stages.preprocess());
        linkedFactories.put(StandardStageDescriptors.QUANTIZE.getName(), stages.quantize());
        linkedFactories.put(StandardStageDescriptors.CONTOUR.getName(), stages.contour());
        linkedFactories.put(StandardStageDescriptors.VECTORIZE.getName(), stages.vectorize());
        linkedFactories.put(StandardStageDescriptors.SPECK_FILTER.getName(), stages.speckFilter());
        linkedFactories.put(StandardStageDescriptors.COLOR_MERGE.getName(), stages.colorMerge());
        linkedFactories.put(StandardStageDescriptors.UNION.getName(), stages.union());
        linkedFactories.put(StandardStageDescriptors.SIMPLIFY.getName(), stages.simplify());
        linkedFactories.put(StandardStageDescriptors.SMOOTH.getName(), stages.smooth());
        linkedFactories.put(StandardStageDescriptors.HOLE_FIX.getName(), stages.holeFix());
        linkedFactories.put(StandardStageDescriptors.LAYER_SORT.getName(), stages.layerSort());
        linkedFactories.put(StandardStageDescriptors.DEDUPE.getName(), stages.dedupe());
        linkedFactories.put(StandardStageDescriptors.NORMALIZE.getName(), stages.normalize());
        linkedFactories.put(StandardStageDescriptors.SERIALIZE.getName(), stages.serialize());
        this.factories=Collections.unmodifiableMap(linkedFactories);
        this.names=Collections.unmodifiableList(new ArrayList<String>(linkedFactories.keySet()));
    }
    public Stage create(String name) {
        StageFactory factory=factories.get(name);
        if (factory==null) {
            throw new IllegalArgumentException("Unknown stage: "+name);
        }
        return factory.create();
    }
    public List<String> names() {
        return names;
    }
    public StageDescriptor getDescriptor(String name) {
        return StandardStageDescriptors.get(name);
    }
    public Pipeline buildDefaultPipeline() {
        return buildPipeline(PipelineConfig.defaults());
    }
    public Pipeline buildPipeline(PipelineConfig config) {
        if (config==null) {
            throw new IllegalArgumentException("config must not be null");
        }
        ArrayList<Stage> stages=new ArrayList<Stage>(names.size());
        for (int index=0;index<names.size();index++) {
            stages.add(create(names.get(index)));
        }
        return new Pipeline(stages, logger, memoizer);
    }
}
