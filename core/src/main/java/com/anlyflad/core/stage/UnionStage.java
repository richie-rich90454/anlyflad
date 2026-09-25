package com.anlyflad.core.stage;
import java.util.ArrayList;
import com.anlyflad.core.model.Color;
import com.anlyflad.core.model.PathId;
import com.anlyflad.core.model.Rect;
import com.anlyflad.core.model.StageDescriptor;
import com.anlyflad.core.model.VectorDocument;
import com.anlyflad.core.model.VectorPath;
import com.anlyflad.core.perf.SpatialHash;
public final class UnionStage implements ConfigurableStage, ColorTransformStage {
    private final StageDescriptor descriptor;
    private final double distance;
    public UnionStage(StageDescriptor descriptor, double distance) {
        if (descriptor==null) {
            throw new IllegalArgumentException("descriptor must not be null");
        }
        if (!Double.isFinite(distance)||distance<0.0) {
            throw new IllegalArgumentException("distance must be finite and nonnegative");
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
        return new UnionStage(descriptor, configured);
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
        ArrayList<Rectangle> rectangles=new ArrayList<Rectangle>(pathCount);
        ArrayList<VectorPath> nonRectangles=new ArrayList<VectorPath>(pathCount);
        for (int index=0;index<pathCount;index++) {
            VectorPath path=document.getPaths().get(index);
            if (isRectangle(path)) {
                rectangles.add(new Rectangle(path));
            } else {
                nonRectangles.add(path);
            }
        }
        boolean changed=true;
        while (changed&&rectangles.size()>1) {
            changed=false;
            SpatialGrid grid=new SpatialGrid(rectangles, distance);
            for (int index=0;index<rectangles.size();index++) {
                Rectangle rectangle=rectangles.get(index);
                if (!rectangle.active) {
                    continue;
                }
                int candidateCount=grid.query(rectangle);
                for (int candidateIndex=0;candidateIndex<candidateCount;candidateIndex++) {
                    int candidateId=grid.getCandidate(candidateIndex);
                    if (candidateId<=index) {
                        continue;
                    }
                    Rectangle candidate=rectangles.get(candidateId);
                    if (!candidate.active||!sameStyle(rectangle, candidate)||!touches(rectangle, candidate)) {
                        continue;
                    }
                    rectangle.bounds=rectangle.bounds.union(candidate.bounds);
                    candidate.active=false;
                    changed=true;
                    break;
                }
            }
        }
        ArrayList<VectorPath> result=new ArrayList<VectorPath>(pathCount);
        int nextId=0;
        for (int index=0;index<nonRectangles.size();index++) {
            VectorPath path=nonRectangles.get(index);
            if (path.isCompound()||path.hasCubicData()) {
                result.add(path);
            } else {
                result.add(new VectorPath(PathId.of(nextId), path.getCoordinates(), path.isClosed(), path.getFill(), path.getOpacity(), path.getFillRule()));
            }
            nextId++;
        }
        for (int index=0;index<rectangles.size();index++) {
            Rectangle rectangle=rectangles.get(index);
            if (!rectangle.active) {
                continue;
            }
            Rect bounds=rectangle.bounds;
            double[] coordinates={bounds.getMinX(), bounds.getMinY(), bounds.getMaxX(), bounds.getMinY(), bounds.getMaxX(), bounds.getMaxY(), bounds.getMinX(), bounds.getMaxY()};
            result.add(new VectorPath(PathId.of(nextId), coordinates, true, rectangle.fill, rectangle.opacity));
            nextId++;
        }
        return document.withPathsAndOwnedPixels(result);
    }
    private boolean touches(Rectangle first, Rectangle second) {
        double horizontalGap=gap(first.bounds.getMinX(), first.bounds.getMaxX(), second.bounds.getMinX(), second.bounds.getMaxX());
        double verticalGap=gap(first.bounds.getMinY(), first.bounds.getMaxY(), second.bounds.getMinY(), second.bounds.getMaxY());
        boolean sameX=first.bounds.getMinX()==second.bounds.getMinX()&&first.bounds.getMaxX()==second.bounds.getMaxX();
        boolean sameY=first.bounds.getMinY()==second.bounds.getMinY()&&first.bounds.getMaxY()==second.bounds.getMaxY();
        return (sameX&&verticalGap<=distance)||(sameY&&horizontalGap<=distance);
    }
    private boolean sameStyle(Rectangle first, Rectangle second) {
        return first.fill.equals(second.fill)&&Double.doubleToLongBits(first.opacity)==Double.doubleToLongBits(second.opacity);
    }
    private double gap(double firstMin, double firstMax, double secondMin, double secondMax) {
        double value=Math.max(firstMin-secondMax, secondMin-firstMax);
        return value<0.0?0.0:value;
    }
    private boolean isRectangle(VectorPath path) {
        if (path.isCompound()||path.hasCubicData()||!path.isClosed()||path.getNodeCount()!=4) {
            return false;
        }
        double[] coordinates=path.getCoordinates();
        return coordinates[0]==coordinates[6]&&coordinates[1]==coordinates[3]&&coordinates[2]==coordinates[4]&&coordinates[5]==coordinates[7];
    }
    private static final class Rectangle {
        private Rect bounds;
        private Color fill;
        private double opacity;
        private boolean active;
        private Rectangle(VectorPath path) {
            this.bounds=path.getBounds();
            this.fill=path.getFill();
            this.opacity=path.getOpacity();
            this.active=true;
        }
    }
    private static final class SpatialGrid {
        private final SpatialHash hash;
        private final int[] candidates;
        private final double distance;
        private SpatialGrid(ArrayList<Rectangle> rectangles, double distance) {
            double minX=rectangles.get(0).bounds.getMinX()-distance-1.0;
            double minY=rectangles.get(0).bounds.getMinY()-distance-1.0;
            double maxX=rectangles.get(0).bounds.getMaxX()+distance+1.0;
            double maxY=rectangles.get(0).bounds.getMaxY()+distance+1.0;
            for (int index=1;index<rectangles.size();index++) {
                minX=Math.min(minX, rectangles.get(index).bounds.getMinX()-distance-1.0);
                minY=Math.min(minY, rectangles.get(index).bounds.getMinY()-distance-1.0);
                maxX=Math.max(maxX, rectangles.get(index).bounds.getMaxX()+distance+1.0);
                maxY=Math.max(maxY, rectangles.get(index).bounds.getMaxY()+distance+1.0);
            }
            int grid=(int)Math.sqrt(rectangles.size());
            if (grid<1) {
                grid=1;
            }
            hash=new SpatialHash(minX, minY, maxX, maxY, grid, rectangles.size()*4+16);
            for (int index=0;index<rectangles.size();index++) {
                Rectangle rectangle=rectangles.get(index);
                hash.insert(index, rectangle.bounds.getMinX(), rectangle.bounds.getMinY(), rectangle.bounds.getMaxX(), rectangle.bounds.getMaxY());
            }
            candidates=new int[rectangles.size()*4+16];
            this.distance=distance;
        }
        private int query(Rectangle rectangle) {
            return hash.query(rectangle.bounds.getMinX()-distance, rectangle.bounds.getMinY()-distance, rectangle.bounds.getMaxX()+distance, rectangle.bounds.getMaxY()+distance, candidates);
        }
        private int getCandidate(int index) {
            return candidates[index];
        }
    }
}
