package com.vectorium.core.perf;
public final class SquaredDeltaTable {
    private static final int[] TABLE=new int[65536];
    static {
        for (int first=0;first<256;first++) {
            for (int second=0;second<256;second++) {
                int delta=first-second;
                TABLE[(first<<8)|second]=delta*delta;
            }
        }
    }
    private SquaredDeltaTable() {
    }
    public static int get(int first, int second) {
        validateChannel(first);
        validateChannel(second);
        return TABLE[(first<<8)|second];
    }
    private static void validateChannel(int value) {
        if (value<0||value>255) {
            throw new IllegalArgumentException("color channels must be between 0 and 255");
        }
    }
}
