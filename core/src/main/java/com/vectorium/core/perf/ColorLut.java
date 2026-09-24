package com.vectorium.core.perf;
public final class ColorLut {
    private static final int ENTRY_COUNT=32768;
    private final int[] entries;
    public ColorLut(int[] palette) {
        if (palette==null||palette.length==0) {
            throw new IllegalArgumentException("palette must not be null or empty");
        }
        entries=new int[ENTRY_COUNT];
        for (int redIndex=0;redIndex<32;redIndex++) {
            int red=representative(redIndex);
            for (int greenIndex=0;greenIndex<32;greenIndex++) {
                int green=representative(greenIndex);
                for (int blueIndex=0;blueIndex<32;blueIndex++) {
                    int blue=representative(blueIndex);
                    int bestPaletteEntry=palette[0]&0xFFFFFF;
                    int bestDistance=Integer.MAX_VALUE;
                    for (int paletteIndex=0;paletteIndex<palette.length;paletteIndex++) {
                        int candidate=palette[paletteIndex];
                        int candidateRed=(candidate>>>16)&0xFF;
                        int candidateGreen=(candidate>>>8)&0xFF;
                        int candidateBlue=candidate&0xFF;
                        int distance=SquaredDeltaTable.get(red, candidateRed)+SquaredDeltaTable.get(green, candidateGreen)+SquaredDeltaTable.get(blue, candidateBlue);
                        if (distance<bestDistance) {
                            bestDistance=distance;
                            bestPaletteEntry=candidate&0xFFFFFF;
                        }
                    }
                    entries[(redIndex<<10)|(greenIndex<<5)|blueIndex]=bestPaletteEntry;
                }
            }
        }
    }
    public int lookup(int red, int green, int blue) {
        validateChannel(red);
        validateChannel(green);
        validateChannel(blue);
        return entries[(red>>>3<<10)|(green>>>3<<5)|(blue>>>3)];
    }
    public int lookup(int rgb) {
        int red=(rgb>>>16)&0xFF;
        int green=(rgb>>>8)&0xFF;
        int blue=rgb&0xFF;
        return lookup(red, green, blue);
    }
    public int lookupArgb(int argb) {
        return (argb&0xFF000000)|lookup(argb);
    }
    private static int representative(int index) {
        return (index<<3)|(index>>2);
    }
    private static void validateChannel(int value) {
        if (value<0||value>255) {
            throw new IllegalArgumentException("color channels must be between 0 and 255");
        }
    }
}
