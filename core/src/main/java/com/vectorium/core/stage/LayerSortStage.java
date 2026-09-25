package com.vectorium.core.stage;
import java.util.ArrayList;
import com.vectorium.core.model.StageDescriptor;
import com.vectorium.core.model.VectorDocument;
import com.vectorium.core.model.VectorPath;
public final class LayerSortStage implements ConfigurableStage, ColorTransformStage {
    private final StageDescriptor descriptor;
    private final boolean descending;
    public LayerSortStage(StageDescriptor descriptor, boolean descending) {
        if (descriptor==null) {
            throw new IllegalArgumentException("descriptor must not be null");
        }
        this.descriptor=descriptor;
        this.descending=descending;
    }
    public Stage withConfig(PipelineConfig config) {
        if (config==null) {
            throw new IllegalArgumentException("config must not be null");
        }
        boolean configured=config.getBoolean(descriptor.getName(), "descending", descending);
        if (configured==descending) {
            return this;
        }
        return new LayerSortStage(descriptor, configured);
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
        return StageTag.CLEANER;
    }
    public boolean appliesTo(VectorDocument document) {
        return document!=null;
    }
    public VectorDocument apply(VectorDocument document) {
        ArrayList<VectorPath> paths=new ArrayList<VectorPath>(document.getPaths());
        paths.sort(new VectorPathAreaComparator(descending));
        return document.withPathsAndOwnedPixels(paths);
    }
}
