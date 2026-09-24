package com.vectorium.core.stage;
import com.vectorium.core.model.VectorDocument;
public interface Stage {
    String getName();
    String getLabel();
    String getDescription();
    StageTag getTag();
    boolean appliesTo(VectorDocument document);
    VectorDocument apply(VectorDocument document) throws StageException;
}
