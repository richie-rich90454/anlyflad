package com.vectorium.core.stage;
import java.util.Comparator;
import com.vectorium.core.model.VectorPath;
public final class VectorPathAreaComparator implements Comparator<VectorPath> {
    private final boolean descending;
    public VectorPathAreaComparator(boolean descending) {
        this.descending=descending;
    }
    public int compare(VectorPath first, VectorPath second) {
        int comparison=Double.compare(first.getArea(), second.getArea());
        if (descending) {
            comparison=-comparison;
        }
        if (comparison!=0) {
            return comparison;
        }
        return first.getId().getValue()-second.getId().getValue();
    }
}
