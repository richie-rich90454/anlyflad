package com.anlyflad.core.stage;
import java.util.ArrayList;
import com.anlyflad.core.model.StageDescriptor;
import com.anlyflad.core.model.VectorDocument;
import com.anlyflad.core.model.VectorPath;
import com.anlyflad.core.perf.SpatialHash;
public final class DedupeStage implements ConfigurableStage, ColorTransformStage {
    private final StageDescriptor descriptor;
    private final double tolerance;
    public DedupeStage(StageDescriptor descriptor, double tolerance) {
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
        return new DedupeStage(descriptor, configured);
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
        int pathCount=document.getPaths().size();
        if (pathCount<2) {
            return document;
        }
        VectorPath first=document.getPaths().get(0);
        double minX=first.getBounds().getMinX()-tolerance-1.0;
        double minY=first.getBounds().getMinY()-tolerance-1.0;
        double maxX=first.getBounds().getMaxX()+tolerance+1.0;
        double maxY=first.getBounds().getMaxY()+tolerance+1.0;
        for (int index=1;index<pathCount;index++) {
            minX=Math.min(minX, document.getPaths().get(index).getBounds().getMinX()-tolerance-1.0);
            minY=Math.min(minY, document.getPaths().get(index).getBounds().getMinY()-tolerance-1.0);
            maxX=Math.max(maxX, document.getPaths().get(index).getBounds().getMaxX()+tolerance+1.0);
            maxY=Math.max(maxY, document.getPaths().get(index).getBounds().getMaxY()+tolerance+1.0);
        }
        int grid=(int)Math.sqrt(pathCount);
        if (grid<1) {
            grid=1;
        }
        SpatialHash spatialHash=new SpatialHash(minX, minY, maxX, maxY, grid, pathCount*4+16);
        for (int index=0;index<pathCount;index++) {
            VectorPath path=document.getPaths().get(index);
            spatialHash.insert(index, path.getBounds().getMinX(), path.getBounds().getMinY(), path.getBounds().getMaxX(), path.getBounds().getMaxY());
        }
        boolean[] duplicate=new boolean[pathCount];
        int[] candidates=new int[pathCount*4+16];
        for (int index=0;index<pathCount;index++) {
            VectorPath path=document.getPaths().get(index);
            int count=spatialHash.query(path.getBounds().getMinX()-tolerance, path.getBounds().getMinY()-tolerance, path.getBounds().getMaxX()+tolerance, path.getBounds().getMaxY()+tolerance, candidates);
            for (int candidateIndex=0;candidateIndex<count;candidateIndex++) {
                int candidateId=candidates[candidateIndex];
                if (candidateId>index&&!duplicate[candidateId]&&equivalent(path, document.getPaths().get(candidateId))) {
                    duplicate[candidateId]=true;
                    break;
                }
            }
        }
        ArrayList<VectorPath> unique=new ArrayList<VectorPath>(pathCount);
        for (int index=0;index<pathCount;index++) {
            if (!duplicate[index]) {
                unique.add(document.getPaths().get(index));
            }
        }
        return document.withPathsAndOwnedPixels(unique);
    }
    private boolean equivalent(VectorPath first, VectorPath second) {
        if (first.isClosed()!=second.isClosed()||first.isCompound()!=second.isCompound()||first.getNodeCount()!=second.getNodeCount()||first.getFill().toArgb()!=second.getFill().toArgb()||first.getFillRule()!=second.getFillRule()||Double.doubleToLongBits(first.getOpacity())!=Double.doubleToLongBits(second.getOpacity())) {
            return false;
        }
        double[][] firstRings=first.getRingCoordinates();
        double[][] secondRings=second.getRingCoordinates();
        if (!ringsEquivalent(firstRings, secondRings)) {
            return false;
        }
        return cubicEquivalent(first.getCubicRingCoordinates(), second.getCubicRingCoordinates());
    }
    private boolean ringsEquivalent(double[][] first, double[][] second) {
        if (first.length!=second.length) {
            return false;
        }
        for (int ringIndex=0;ringIndex<first.length;ringIndex++) {
            if (!valuesEquivalent(first[ringIndex], second[ringIndex])) {
                return false;
            }
        }
        return true;
    }
    private boolean cubicEquivalent(double[][] first, double[][] second) {
        boolean firstPresent=first.length>0;
        boolean secondPresent=second.length>0;
        if (firstPresent!=secondPresent) {
            return false;
        }
        if (!firstPresent) {
            return true;
        }
        if (first.length!=second.length) {
            return false;
        }
        for (int ringIndex=0;ringIndex<first.length;ringIndex++) {
            if ((first[ringIndex]==null)!=(second[ringIndex]==null)) {
                return false;
            }
            if (first[ringIndex]!=null&&!valuesEquivalent(first[ringIndex], second[ringIndex])) {
                return false;
            }
        }
        return true;
    }
    private boolean valuesEquivalent(double[] first, double[] second) {
        if (first.length!=second.length) {
            return false;
        }
        for (int index=0;index<first.length;index++) {
            if (Math.abs(first[index]-second[index])>tolerance) {
                return false;
            }
        }
        return true;
    }
}
