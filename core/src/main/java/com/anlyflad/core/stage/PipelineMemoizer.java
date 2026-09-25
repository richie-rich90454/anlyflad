package com.anlyflad.core.stage;
import com.anlyflad.core.model.VectorDocument;
public interface PipelineMemoizer {
    VectorDocument get(VectorDocument input, long configHash);
    void put(VectorDocument input, long configHash, VectorDocument output);
    void clear();
}
