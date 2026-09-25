package com.anlyflad.core.stage;
import java.util.Locale;
public enum VectorMode {
    EXACT("exact"),
    CONTOUR("contour"),
    CURVE("curve");
    private final String optionName;
    VectorMode(String optionName) {
        this.optionName=optionName;
    }
    public String getOptionName() {
        return optionName;
    }
    public static VectorMode parse(String value) {
        if (value==null||value.trim().isEmpty()) {
            throw new IllegalArgumentException("vector mode must be exact, contour, or curve");
        }
        String normalized=value.trim().toLowerCase(Locale.ROOT);
        if (normalized.equals("exact")||normalized.equals("lossless")||normalized.equals("run")) {
            return EXACT;
        }
        if (normalized.equals("contour")||normalized.equals("region")||normalized.equals("compact")) {
            return CONTOUR;
        }
        if (normalized.equals("curve")||normalized.equals("curves")||normalized.equals("fitted")) {
            return CURVE;
        }
        throw new IllegalArgumentException("vector mode must be exact, contour, or curve");
    }
}
