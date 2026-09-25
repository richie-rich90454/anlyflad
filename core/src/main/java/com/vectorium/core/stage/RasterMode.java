package com.vectorium.core.stage;
import java.util.Locale;
public enum RasterMode {
    BINARY("binary"),
    COLOR("color");
    private final String optionName;
    RasterMode(String optionName) {
        this.optionName=optionName;
    }
    public String getOptionName() {
        return optionName;
    }
    public static RasterMode parse(String value) {
        if (value==null||value.trim().isEmpty()) {
            throw new IllegalArgumentException("raster mode must be color or binary");
        }
        String normalized=value.trim().toLowerCase(Locale.ROOT);
        if (normalized.equals("color")||normalized.equals("exact")||normalized.equals("full-color")) {
            return COLOR;
        }
        if (normalized.equals("binary")||normalized.equals("monochrome")) {
            return BINARY;
        }
        throw new IllegalArgumentException("raster mode must be color or binary");
    }
}
