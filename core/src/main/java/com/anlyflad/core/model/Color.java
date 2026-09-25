package com.anlyflad.core.model;
public final class Color {
    private final int red;
    private final int green;
    private final int blue;
    private final int alpha;
    public Color(int red, int green, int blue, int alpha) {
        validateChannel(red, "red");
        validateChannel(green, "green");
        validateChannel(blue, "blue");
        validateChannel(alpha, "alpha");
        this.red=red;
        this.green=green;
        this.blue=blue;
        this.alpha=alpha;
    }
    public static Color fromArgb(int argb) {
        int alpha=(argb>>>24)&0xFF;
        int red=(argb>>>16)&0xFF;
        int green=(argb>>>8)&0xFF;
        int blue=argb&0xFF;
        return new Color(red, green, blue, alpha);
    }
    public static Color fromHex(String hex) {
        if (hex==null||hex.length()<4||hex.charAt(0)!='#') {
            throw new IllegalArgumentException("Hex color must start with # and contain three, four, six, or eight digits");
        }
        int digitCount=hex.length()-1;
        if (digitCount==3||digitCount==4) {
            int red=expandDigit(digit(hex, 1));
            int green=expandDigit(digit(hex, 2));
            int blue=expandDigit(digit(hex, 3));
            int alpha=255;
            if (digitCount==4) {
                alpha=expandDigit(digit(hex, 4));
            }
            return new Color(red, green, blue, alpha);
        }
        if (digitCount==6||digitCount==8) {
            int red=parseByte(hex, 1);
            int green=parseByte(hex, 3);
            int blue=parseByte(hex, 5);
            int alpha=255;
            if (digitCount==8) {
                alpha=parseByte(hex, 7);
            }
            return new Color(red, green, blue, alpha);
        }
        throw new IllegalArgumentException("Hex color must contain three, four, six, or eight digits");
    }
    public int getRed() {
        return red;
    }
    public int getGreen() {
        return green;
    }
    public int getBlue() {
        return blue;
    }
    public int getAlpha() {
        return alpha;
    }
    public int toArgb() {
        return (alpha<<24)|(red<<16)|(green<<8)|blue;
    }
    @Override
    public boolean equals(Object other) {
        if (this==other) {
            return true;
        }
        if (other==null||getClass()!=other.getClass()) {
            return false;
        }
        Color color=(Color)other;
        return red==color.red&&green==color.green&&blue==color.blue&&alpha==color.alpha;
    }
    @Override
    public int hashCode() {
        int result=17;
        result=31*result+red;
        result=31*result+green;
        result=31*result+blue;
        result=31*result+alpha;
        return result;
    }
    @Override
    public String toString() {
        return "Color{red="+red+", green="+green+", blue="+blue+", alpha="+alpha+"}";
    }
    private static void validateChannel(int value, String name) {
        if (value<0||value>255) {
            throw new IllegalArgumentException(name+" must be between 0 and 255");
        }
    }
    private static int digit(String value, int index) {
        int digit=Character.digit(value.charAt(index), 16);
        if (digit<0) {
            throw new IllegalArgumentException("Hex color contains a non-hexadecimal digit");
        }
        return digit;
    }
    private static int expandDigit(int digit) {
        return (digit<<4)|digit;
    }
    private static int parseByte(String value, int index) {
        return (digit(value, index)<<4)|digit(value, index+1);
    }
}
