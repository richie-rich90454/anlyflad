package com.vectorium.core.stage;
import java.util.ArrayList;
import com.vectorium.core.geometry.GeometryMath;
import com.vectorium.core.model.StageDescriptor;
import com.vectorium.core.model.VectorDocument;
import com.vectorium.core.model.VectorPath;
public final class HoleFixStage implements ConfigurableStage, ColorTransformStage {
    private final StageDescriptor descriptor;
    private final double maxArea;
    public HoleFixStage(StageDescriptor descriptor, double maxArea) {
        if (descriptor==null) {
            throw new IllegalArgumentException("descriptor must not be null");
        }
        if (!Double.isFinite(maxArea)||maxArea<=0.0) {
            throw new IllegalArgumentException("maxArea must be finite and positive");
        }
        this.descriptor=descriptor;
        this.maxArea=maxArea;
    }
    public Stage withConfig(PipelineConfig config) {
        if (config==null) {
            throw new IllegalArgumentException("config must not be null");
        }
        double configured=config.getDouble(descriptor.getName(), "maxArea", maxArea);
        if (Double.doubleToLongBits(configured)==Double.doubleToLongBits(maxArea)) {
            return this;
        }
        return new HoleFixStage(descriptor, configured);
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
        ArrayList<VectorPath> fixed=new ArrayList<VectorPath>(document.getPaths().size());
        for (int index=0;index<document.getPaths().size();index++) {
            VectorPath path=document.getPaths().get(index);
            if (!path.isCompound()&&!path.hasCubicData()&&path.isClosed()&&path.getArea()<=maxArea&&isContained(path, document, index)) {
                continue;
            }
            fixed.add(path);
        }
        return document.withPathsAndOwnedPixels(fixed);
    }
    private boolean isContained(VectorPath path, VectorDocument document, int pathIndex) {
        double[] coordinates=path.getCoordinates();
        for (int candidateIndex=0;candidateIndex<document.getPaths().size();candidateIndex++) {
            if (candidateIndex==pathIndex) {
                continue;
            }
            VectorPath candidate=document.getPaths().get(candidateIndex);
            if (candidate.getArea()<=path.getArea()||!candidate.isClosed()) {
                continue;
            }
            boolean contains=true;
            for (int coordinate=0;coordinate<coordinates.length&&contains;coordinate+=2) {
                contains=GeometryMath.contains(candidate.getCoordinates(), coordinates[coordinate], coordinates[coordinate+1]);
            }
            if (contains) {
                return true;
            }
        }
        return false;
    }
}
