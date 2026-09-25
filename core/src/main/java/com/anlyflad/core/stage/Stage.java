package com.anlyflad.core.stage;
import com.anlyflad.core.model.VectorDocument;
public interface Stage {
    String getName();
    String getLabel();
    String getDescription();
    StageTag getTag();
    boolean appliesTo(VectorDocument document);
    VectorDocument apply(VectorDocument document) throws StageException;
}
