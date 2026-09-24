package com.vectorium.core.stage;
import java.util.ArrayList;
import com.vectorium.core.model.StageDescriptor;
import com.vectorium.core.model.VectorDocument;
import com.vectorium.core.model.VectorPath;
public final class NormalizeStage implements ConfigurableStage {
    private final StageDescriptor descriptor;
    private final boolean enabled;
    public NormalizeStage(StageDescriptor descriptor, boolean enabled) {
        if (descriptor==null) {
            throw new IllegalArgumentException("descriptor must not be null");
        }
        this.descriptor=descriptor;
        this.enabled=enabled;
    }
    public Stage withConfig(PipelineConfig config) {
        if (config==null) {
            throw new IllegalArgumentException("config must not be null");
        }
        boolean configured=config.getBoolean(descriptor.getName(), "enabled", enabled);
        if (configured==enabled) {
            return this;
        }
        return new NormalizeStage(descriptor, configured);
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
        if (!enabled||document.getPaths().isEmpty()) {
            return document;
        }
        double minX=document.getPaths().get(0).getBounds().getMinX();
        double minY=document.getPaths().get(0).getBounds().getMinY();
        double maxX=document.getPaths().get(0).getBounds().getMaxX();
        double maxY=document.getPaths().get(0).getBounds().getMaxY();
        for (int index=1;index<document.getPaths().size();index++) {
            minX=Math.min(minX, document.getPaths().get(index).getBounds().getMinX());
            minY=Math.min(minY, document.getPaths().get(index).getBounds().getMinY());
            maxX=Math.max(maxX, document.getPaths().get(index).getBounds().getMaxX());
            maxY=Math.max(maxY, document.getPaths().get(index).getBounds().getMaxY());
        }
        ArrayList<VectorPath> normalized=new ArrayList<VectorPath>(document.getPaths().size());
        for (int index=0;index<document.getPaths().size();index++) {
            VectorPath path=document.getPaths().get(index);
            double[] source=path.getCoordinates();
            double[] coordinates=new double[source.length];
            for (int coordinate=0;coordinate<source.length;coordinate+=2) {
                coordinates[coordinate]=source[coordinate]-minX;
                coordinates[coordinate+1]=source[coordinate+1]-minY;
            }
            normalized.add(path.withGeometry(coordinates, path.isClosed()));
        }
        int width=dimension(maxX-minX);
        int height=dimension(maxY-minY);
        if (document.getOrigin().isRaster()) {
            int[] pixels=new int[width*height];
            for (int y=0;y<height;y++) {
                int sourceY=(int)Math.round(minY)+y;
                if (sourceY<0||sourceY>=document.getHeight()) {
                    continue;
                }
                for (int x=0;x<width;x++) {
                    int sourceX=(int)Math.round(minX)+x;
                    if (sourceX<0||sourceX>=document.getWidth()) {
                        continue;
                    }
                    pixels[y*width+x]=document.getOwnedPixels()[sourceY*document.getWidth()+sourceX];
                }
            }
            return new VectorDocument(document.getDocumentId(), document.getOrigin(), normalized, width, height, pixels);
        }
        return document.withPaths(normalized).withSize(width, height);
    }
    private int dimension(double value) throws StageException {
        if (!Double.isFinite(value)||value<0.0||value>Integer.MAX_VALUE) {
            throw new StageException("normalized dimensions must be finite and nonnegative");
        }
        int dimension=(int)Math.ceil(value);
        return dimension<1?1:dimension;
    }
}
