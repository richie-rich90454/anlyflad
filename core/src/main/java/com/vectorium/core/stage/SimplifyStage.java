package com.vectorium.core.stage;
import java.util.ArrayList;
import com.vectorium.core.geometry.DouglasPeucker;
import com.vectorium.core.model.StageDescriptor;
import com.vectorium.core.model.VectorDocument;
import com.vectorium.core.model.VectorPath;
public final class SimplifyStage implements ConfigurableStage, ColorTransformStage {
    private final StageDescriptor descriptor;
    private final double tolerance;
    public SimplifyStage(StageDescriptor descriptor, double tolerance) {
        if (descriptor==null) {
            throw new IllegalArgumentException("descriptor must not be null");
        }
        if (!Double.isFinite(tolerance)||tolerance<0.0) {
            throw new IllegalArgumentException("tolerance must be finite and nonnegative");
        }
        this.descriptor=descriptor;
        this.tolerance=tolerance;
    }
    public Stage withConfig(PipelineConfig config) {
        if (config==null) {
            throw new IllegalArgumentException("config must not be null");
        }
        double configured=config.getDouble(descriptor.getName(), "tolerance", tolerance);
        if (Double.doubleToLongBits(configured)==Double.doubleToLongBits(tolerance)) {
            return this;
        }
        return new SimplifyStage(descriptor, configured);
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
        ArrayList<VectorPath> simplified=new ArrayList<VectorPath>(document.getPaths().size());
        for (int index=0;index<document.getPaths().size();index++) {
            VectorPath path=document.getPaths().get(index);
            if (path.isCompound()||path.hasCubicData()) {
                simplified.add(path);
                continue;
            }
            int pointCount=path.getNodeCount();
            if (pointCount<=2) {
                simplified.add(path);
                continue;
            }
            double[] coordinates=path.getCoordinates();
            boolean[] keep=new boolean[pointCount];
            int[] stack=new int[pointCount*2];
            int[] outputIndices=new int[pointCount];
            int count=DouglasPeucker.simplify(coordinates, path.isClosed(), tolerance, keep, stack, outputIndices);
            if (path.isClosed()&&count<3) {
                simplified.add(path);
                continue;
            }
            double[] simplifiedCoordinates=new double[count*2];
            for (int outputIndex=0;outputIndex<count;outputIndex++) {
                int sourceIndex=outputIndices[outputIndex];
                simplifiedCoordinates[outputIndex*2]=coordinates[sourceIndex*2];
                simplifiedCoordinates[outputIndex*2+1]=coordinates[sourceIndex*2+1];
            }
            simplified.add(path.withGeometry(simplifiedCoordinates, path.isClosed()));
        }
        return document.withPathsAndOwnedPixels(simplified);
    }
}
