package com.anlyflad.core.stage;
import java.util.Locale;
public final class QualityScale {
    public static final int MINIMUM=0;
    public static final int MAXIMUM=100;
    public static final int DEFAULT=50;
    private static final double DRAFT_TOLERANCE=1.2;
    private static final double BALANCED_TOLERANCE=0.35;
    private static final double MAX_TOLERANCE=0.05;
    private static final int DRAFT_COLORS=6;
    private static final int BALANCED_COLORS=16;
    private static final int MAX_COLORS=32;
    private QualityScale() {
    }
    public static int parse(String value) {
        if (value==null||value.trim().isEmpty()) {
            throw new IllegalArgumentException("quality must be 0 to 100, or draft, balanced, or max");
        }
        String normalized=value.trim().toLowerCase(Locale.ROOT);
        if (normalized.equals("draft")) {
            return MINIMUM;
        }
        if (normalized.equals("balanced")) {
            return DEFAULT;
        }
        if (normalized.equals("max")) {
            return MAXIMUM;
        }
        int parsed;
        try {
            parsed=Integer.parseInt(normalized);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("quality must be 0 to 100, or draft, balanced, or max", exception);
        }
        if (parsed<MINIMUM||parsed>MAXIMUM) {
            throw new IllegalArgumentException("quality must be between "+MINIMUM+" and "+MAXIMUM);
        }
        return parsed;
    }
    public static double curveTolerance(int quality) {
        requireRange(quality);
        if (quality<=DEFAULT) {
            return DRAFT_TOLERANCE+(BALANCED_TOLERANCE-DRAFT_TOLERANCE)*(quality/(double)DEFAULT);
        }
        return BALANCED_TOLERANCE+(MAX_TOLERANCE-BALANCED_TOLERANCE)*((quality-DEFAULT)/(double)(MAXIMUM-DEFAULT));
    }
    public static int supersample(int quality) {
        requireRange(quality);
        if (quality<20) {
            return 1;
        }
        if (quality<45) {
            return 2;
        }
        if (quality<80) {
            return 0;
        }
        return 4;
    }
    public static int maxColors(int quality) {
        requireRange(quality);
        if (quality<=DEFAULT) {
            return DRAFT_COLORS+(int)Math.round((BALANCED_COLORS-DRAFT_COLORS)*(quality/(double)DEFAULT));
        }
        return BALANCED_COLORS+(int)Math.round((MAX_COLORS-BALANCED_COLORS)*((quality-DEFAULT)/(double)(MAXIMUM-DEFAULT)));
    }
    private static void requireRange(int quality) {
        if (quality<MINIMUM||quality>MAXIMUM) {
            throw new IllegalArgumentException("quality must be between "+MINIMUM+" and "+MAXIMUM);
        }
    }
}
