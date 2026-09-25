package com.anlyflad.core.perf;
public final class MortonCodes {
    private MortonCodes() {
    }
    public static long pack(int x, int y) {
        return spread(x)|(spread(y)<<1);
    }
    public static int unpackX(long code) {
        return compact(code);
    }
    public static int unpackY(long code) {
        return compact(code>>>1);
    }
    public static int sort(long[] values, int length, long[] scratch) {
        validatePrefix(values, length);
        if (scratch==null||scratch.length<length) {
            throw new IllegalArgumentException("scratch must cover the prefix");
        }
        if (scratch==values) {
            throw new IllegalArgumentException("scratch must be separate from values");
        }
        if (length<2) {
            return length;
        }
        long[] source=values;
        long[] destination=scratch;
        int width=1;
        while (width<length) {
            int start=0;
            while (start<length) {
                int middle=start+width;
                if (middle>length) {
                    middle=length;
                }
                int end=middle+width;
                if (end>length) {
                    end=length;
                }
                merge(source, destination, start, middle, end);
                start+=width*2;
            }
            long[] swap=source;
            source=destination;
            destination=swap;
            if (width>length/2) {
                break;
            }
            width*=2;
        }
        if (source==scratch) {
            for (int index=0;index<length;index++) {
                values[index]=scratch[index];
            }
        }
        return length;
    }
    public static int search(long[] values, int length, int x, int y) {
        validatePrefix(values, length);
        long key=pack(x, y);
        int low=0;
        int high=length;
        while (low<high) {
            int middle=low+(high-low)/2;
            if (Long.compareUnsigned(values[middle], key)<0) {
                low=middle+1;
            } else {
                high=middle;
            }
        }
        if (low<length&&Long.compareUnsigned(values[low], key)==0) {
            return low;
        }
        return -1;
    }
    private static long spread(int value) {
        long bits=value&0xFFFFFFFFL;
        bits=(bits|(bits<<16))&0x0000FFFF0000FFFFL;
        bits=(bits|(bits<<8))&0x00FF00FF00FF00FFL;
        bits=(bits|(bits<<4))&0x0F0F0F0F0F0F0F0FL;
        bits=(bits|(bits<<2))&0x3333333333333333L;
        bits=(bits|(bits<<1))&0x5555555555555555L;
        return bits;
    }
    private static int compact(long value) {
        value&=0x5555555555555555L;
        value=(value^(value>>>1))&0x3333333333333333L;
        value=(value^(value>>>2))&0x0F0F0F0F0F0F0F0FL;
        value=(value^(value>>>4))&0x00FF00FF00FF00FFL;
        value=(value^(value>>>8))&0x0000FFFF0000FFFFL;
        value=(value^(value>>>16))&0x00000000FFFFFFFFL;
        return (int)value;
    }
    private static void merge(long[] source, long[] destination, int start, int middle, int end) {
        int left=start;
        int right=middle;
        int output=start;
        while (left<middle&&right<end) {
            if (Long.compareUnsigned(source[left], source[right])<=0) {
                destination[output]=source[left];
                left++;
            } else {
                destination[output]=source[right];
                right++;
            }
            output++;
        }
        while (left<middle) {
            destination[output]=source[left];
            left++;
            output++;
        }
        while (right<end) {
            destination[output]=source[right];
            right++;
            output++;
        }
    }
    private static void validatePrefix(long[] values, int length) {
        if (values==null) {
            throw new IllegalArgumentException("values must not be null");
        }
        if (length<0||length>values.length) {
            throw new IllegalArgumentException("length is outside the values array");
        }
    }
}
