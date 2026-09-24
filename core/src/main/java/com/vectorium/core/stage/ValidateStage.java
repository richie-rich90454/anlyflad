package com.vectorium.core.stage;
import com.vectorium.core.model.StageDescriptor;
import com.vectorium.core.model.VectorDocument;
import com.vectorium.core.model.VectorPath;
public final class ValidateStage implements ConfigurableStage {
    private final StageDescriptor descriptor;
    public ValidateStage(StageDescriptor descriptor) {
        if (descriptor==null) {
            throw new IllegalArgumentException("descriptor must not be null");
        }
        this.descriptor=descriptor;
    }
    public Stage withConfig(PipelineConfig config) {
        if (config==null) {
            throw new IllegalArgumentException("config must not be null");
        }
        return this;
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
        if (document.getWidth()<0||document.getHeight()<0) {
            throw new StageException("document dimensions must be nonnegative");
        }
        long pixelLength=(long)document.getWidth()*document.getHeight();
        if (pixelLength>Integer.MAX_VALUE||document.getOwnedPixels().length!=(int)pixelLength) {
            throw new StageException("document pixel buffer does not match its dimensions");
        }
        for (int index=0;index<document.getPaths().size();index++) {
            VectorPath path=document.getPaths().get(index);
            if (path==null) {
                throw new StageException("document contains a null path");
            }
        }
        if (document.getOrigin().isRaster()&&(document.getWidth()==0||document.getHeight()==0)) {
            throw new StageException("raster documents require positive dimensions");
        }
        return document;
    }
}
