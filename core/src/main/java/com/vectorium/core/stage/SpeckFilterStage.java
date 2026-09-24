package com.vectorium.core.stage;
import java.util.ArrayList;
import java.util.List;
import com.vectorium.core.model.StageDescriptor;
import com.vectorium.core.model.VectorDocument;
import com.vectorium.core.model.VectorPath;
public final class SpeckFilterStage implements ConfigurableStage {
    private final StageDescriptor descriptor;
    private final double minArea;
    public SpeckFilterStage(StageDescriptor descriptor, double minArea) {
        if (descriptor==null) {
            throw new IllegalArgumentException("descriptor must not be null");
        }
        if (!Double.isFinite(minArea)||minArea<0.0) {
            throw new IllegalArgumentException("minArea must be finite and nonnegative");
        }
        this.descriptor=descriptor;
        this.minArea=minArea;
    }
    public Stage withConfig(PipelineConfig config) {
        if (config==null) {
            throw new IllegalArgumentException("config must not be null");
        }
        double configured=config.getDouble(descriptor.getName(), "minArea", minArea);
        if (Double.doubleToLongBits(configured)==Double.doubleToLongBits(minArea)) {
            return this;
        }
        return new SpeckFilterStage(descriptor, configured);
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
        List<VectorPath> filtered=new ArrayList<VectorPath>(document.getPaths().size());
        for (int index=0;index<document.getPaths().size();index++) {
            VectorPath path=document.getPaths().get(index);
            if (path.getArea()>=minArea) {
                filtered.add(path);
            }
        }
        return document.withPaths(filtered);
    }
}
