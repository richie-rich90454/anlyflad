package com.vectorium.core.stage;
import java.util.ArrayList;
import com.vectorium.core.model.StageDescriptor;
import com.vectorium.core.model.VectorDocument;
import com.vectorium.core.model.VectorPath;
public final class SmoothStage implements ConfigurableStage, ColorTransformStage {
    private static final double RATIO=0.25;
    private final StageDescriptor descriptor;
    private final int passes;
    public SmoothStage(StageDescriptor descriptor, int passes) {
        if (descriptor==null) {
            throw new IllegalArgumentException("descriptor must not be null");
        }
        if (passes<0||passes>3) {
            throw new IllegalArgumentException("passes must be between zero and three");
        }
        this.descriptor=descriptor;
        this.passes=passes;
    }
    public Stage withConfig(PipelineConfig config) {
        if (config==null) {
            throw new IllegalArgumentException("config must not be null");
        }
        int configured=config.getInteger(descriptor.getName(), "passes", passes);
        if (configured==passes) {
            return this;
        }
        return new SmoothStage(descriptor, configured);
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
        ArrayList<VectorPath> smoothed=new ArrayList<VectorPath>(document.getPaths().size());
        for (int index=0;index<document.getPaths().size();index++) {
            smoothed.add(smooth(document.getPaths().get(index)));
        }
        return document.withPathsAndOwnedPixels(smoothed);
    }
    private VectorPath smooth(VectorPath path) {
        if (passes==0||path.getNodeCount()<3||path.isCompound()||path.hasCubicData()) {
            return path;
        }
        double[] coordinates=path.getCoordinates();
        int pointCount=path.getNodeCount();
        for (int pass=0;pass<passes;pass++) {
            double[] next=new double[pointCount*4];
            int nextCount=0;
            if (path.isClosed()) {
                for (int index=0;index<pointCount;index++) {
                    int nextIndex=(index+1)%pointCount;
                    appendCorner(next, nextCount, coordinates, index, nextIndex);
                    nextCount+=2;
                }
            } else {
                next[0]=coordinates[0];
                next[1]=coordinates[1];
                nextCount=1;
                for (int index=0;index<pointCount-1;index++) {
                    appendCorner(next, nextCount, coordinates, index, index+1);
                    nextCount+=2;
                }
                next[nextCount*2]=coordinates[coordinates.length-2];
                next[nextCount*2+1]=coordinates[coordinates.length-1];
                nextCount++;
            }
            coordinates=next;
            pointCount=nextCount;
        }
        return path.withGeometry(coordinates, path.isClosed());
    }
    private void appendCorner(double[] output, int outputPoint, double[] input, int index, int nextIndex) {
        double x=input[index*2];
        double y=input[index*2+1];
        double nextX=input[nextIndex*2];
        double nextY=input[nextIndex*2+1];
        output[outputPoint*2]=(1.0-RATIO)*x+RATIO*nextX;
        output[outputPoint*2+1]=(1.0-RATIO)*y+RATIO*nextY;
        output[(outputPoint+1)*2]=RATIO*x+(1.0-RATIO)*nextX;
        output[(outputPoint+1)*2+1]=RATIO*y+(1.0-RATIO)*nextY;
    }
}
