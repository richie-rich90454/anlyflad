package com.vectorium.core.stage;
import java.util.ArrayList;
import java.util.List;
import com.vectorium.core.model.StageDescriptor;
import com.vectorium.core.model.VectorDocument;
import com.vectorium.core.model.VectorPath;
public final class NormalizeStage implements ConfigurableStage, ColorTransformStage {
    private static final long MAX_PIXELS=100L*1024L*1024L;
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
            normalized.add(translate(document.getPaths().get(index), minX, minY));
        }
        int width=dimension(maxX-minX);
        int height=dimension(maxY-minY);
        if (document.getOrigin().isRaster()) {
            long pixelLength=(long)width*(long)height;
            if (pixelLength>MAX_PIXELS||pixelLength>Integer.MAX_VALUE) {
                throw new StageException("normalized dimensions exceed the 100-megapixel limit");
            }
            int[] pixels=new int[(int)pixelLength];
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
            return VectorDocument.fromOwnedPixels(document.getDocumentId(), document.getOrigin(), normalized, width, height, pixels);
        }
        return document.withPathsAndSize(normalized, width, height);
    }
    private VectorPath translate(VectorPath path, double minX, double minY) {
        List<double[]> rings=path.getRings();
        List<double[]> shiftedRings=new ArrayList<double[]>(rings.size());
        for (int index=0;index<rings.size();index++) {
            double[] source=rings.get(index);
            double[] shifted=new double[source.length];
            for (int coordinate=0;coordinate<source.length;coordinate+=2) {
                shifted[coordinate]=source[coordinate]-minX;
                shifted[coordinate+1]=source[coordinate+1]-minY;
            }
            shiftedRings.add(shifted);
        }
        double[][] cubic=path.getCubicRingCoordinates();
        double[][] shiftedCubic=null;
        if (cubic.length>0) {
            shiftedCubic=new double[cubic.length][];
            for (int index=0;index<cubic.length;index++) {
                if (cubic[index]==null) {
                    continue;
                }
                double[] source=cubic[index];
                double[] shifted=new double[source.length];
                for (int coordinate=0;coordinate<source.length;coordinate+=2) {
                    shifted[coordinate]=source[coordinate]-minX;
                    shifted[coordinate+1]=source[coordinate+1]-minY;
                }
                shiftedCubic[index]=shifted;
            }
        }
        return path.withRingsAndArea(shiftedRings, shiftedCubic, path.getArea());
    }
    private int dimension(double value) throws StageException {
        if (!Double.isFinite(value)||value<0.0||value>Integer.MAX_VALUE) {
            throw new StageException("normalized dimensions must be finite and nonnegative");
        }
        int dimension=(int)Math.ceil(value);
        return dimension<1?1:dimension;
    }
}
