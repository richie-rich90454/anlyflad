package com.vectorium.core.stage;
import com.vectorium.core.model.StageDescriptor;
import com.vectorium.core.model.VectorDocument;
import com.vectorium.core.svg.SvgCache;
public final class SerializeStage implements ConfigurableStage {
    private final StageDescriptor descriptor;
    private final SvgCache cache;
    public SerializeStage(StageDescriptor descriptor, SvgCache cache) {
        if (descriptor==null) {
            throw new IllegalArgumentException("descriptor must not be null");
        }
        if (cache==null) {
            throw new IllegalArgumentException("cache must not be null");
        }
        this.descriptor=descriptor;
        this.cache=cache;
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
        return StageTag.UNIVERSAL;
    }
    public boolean appliesTo(VectorDocument document) {
        return document!=null;
    }
    public VectorDocument apply(VectorDocument document) throws StageException {
        if (document==null) {
            throw new StageException("document must not be null");
        }
        try {
            cache.get(document);
            return document;
        } catch (IllegalArgumentException exception) {
            throw StageException.userError(exception.getMessage(), exception);
        }
    }
}
