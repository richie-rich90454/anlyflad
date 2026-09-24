package com.vectorium.core.stage;
import java.util.ArrayList;
import com.vectorium.core.model.StageDescriptor;
import com.vectorium.core.model.VectorDocument;
import com.vectorium.core.model.VectorPath;
import com.vectorium.core.perf.SpatialHash;
public final class ColorMergeStage implements ConfigurableStage {
    private final StageDescriptor descriptor;
    private final double distance;
    public ColorMergeStage(StageDescriptor descriptor, double distance) {
        if (descriptor==null) {
            throw new IllegalArgumentException("descriptor must not be null");
        }
        if (!Double.isFinite(distance)||distance<0.0||distance>441.0) {
            throw new IllegalArgumentException("distance must be finite and between zero and 441");
        }
        this.descriptor=descriptor;
        this.distance=distance;
    }
    public Stage withConfig(PipelineConfig config) {
        if (config==null) {
            throw new IllegalArgumentException("config must not be null");
        }
        double configured=config.getDouble(descriptor.getName(), "distance", distance);
        if (Double.doubleToLongBits(configured)==Double.doubleToLongBits(distance)) {
            return this;
        }
        return new ColorMergeStage(descriptor, configured);
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
        if (pathCount==0) {
            return document;
        }
        VectorPath first=document.getPaths().get(0);
        double minX=first.getBounds().getMinX()-distance-1.0;
        double minY=first.getBounds().getMinY()-distance-1.0;
        double maxX=first.getBounds().getMaxX()+distance+1.0;
        double maxY=first.getBounds().getMaxY()+distance+1.0;
        double averageWidth=0.0;
        double averageHeight=0.0;
        for (int index=0;index<pathCount;index++) {
            VectorPath path=document.getPaths().get(index);
            minX=Math.min(minX, path.getBounds().getMinX()-distance-1.0);
            minY=Math.min(minY, path.getBounds().getMinY()-distance-1.0);
            maxX=Math.max(maxX, path.getBounds().getMaxX()+distance+1.0);
            maxY=Math.max(maxY, path.getBounds().getMaxY()+distance+1.0);
            averageWidth+=Math.max(path.getBounds().getWidth(), 1.0);
            averageHeight+=Math.max(path.getBounds().getHeight(), 1.0);
        }
        averageWidth/=pathCount;
        averageHeight/=pathCount;
        int grid=(int)Math.sqrt(pathCount);
        if (grid<1) {
            grid=1;
        }
        if (grid>128) {
            grid=128;
        }
        SpatialHash spatialHash=new SpatialHash(minX, minY, maxX, maxY, grid, pathCount*4+16);
        for (int index=0;index<pathCount;index++) {
            VectorPath path=document.getPaths().get(index);
            spatialHash.insert(index, path.getBounds().getMinX(), path.getBounds().getMinY(), path.getBounds().getMaxX(), path.getBounds().getMaxY());
        }
        ArrayList<VectorPath> merged=new ArrayList<VectorPath>(pathCount);
        int[] candidates=new int[pathCount*4+16];
        double distanceSquared=distance*distance;
        for (int index=0;index<pathCount;index++) {
            VectorPath path=document.getPaths().get(index);
            int count=spatialHash.query(path.getBounds().getMinX()-distance, path.getBounds().getMinY()-distance, path.getBounds().getMaxX()+distance, path.getBounds().getMaxY()+distance, candidates);
            VectorPath result=path;
            for (int candidateIndex=0;candidateIndex<count;candidateIndex++) {
                int candidateId=candidates[candidateIndex];
                if (candidateId>=index) {
                    continue;
                }
                VectorPath candidate=document.getPaths().get(candidateId);
                if (colorDistanceSquared(path, candidate)<=distanceSquared) {
                    result=path.withStyle(candidate.getFill(), path.getOpacity());
                    break;
                }
            }
            merged.add(result);
        }
        return document.withPaths(merged);
    }
    private double colorDistanceSquared(VectorPath first, VectorPath second) {
        int red=first.getFill().getRed()-second.getFill().getRed();
        int green=first.getFill().getGreen()-second.getFill().getGreen();
        int blue=first.getFill().getBlue()-second.getFill().getBlue();
        return red*red+green*green+blue*blue;
    }
}
