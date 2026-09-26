package com.anlyflad.core.raster;

import java.util.Arrays;
import com.anlyflad.core.perf.ColorLut;

public final class AdaptiveColorQuantizer {
    public static final int DEFAULT_MAX_COLORS=8;
    public static final int MAX_COLORS=256;
    private static final int HISTOGRAM_BITS=6;
    private static final int HISTOGRAM_LEVELS=1<<HISTOGRAM_BITS;
    private static final int HISTOGRAM_SIZE=HISTOGRAM_LEVELS*HISTOGRAM_LEVELS*HISTOGRAM_LEVELS;
    private final int maxColors;

    public AdaptiveColorQuantizer(int maxColors) {
        if (maxColors<=0||maxColors>MAX_COLORS) {
            throw new IllegalArgumentException("maxColors must be between 1 and "+MAX_COLORS);
        }
        this.maxColors=maxColors;
    }

    public int[] buildPalette(int[] argb) {
        if (argb==null) {
            throw new IllegalArgumentException("argb must not be null");
        }
        Histogram histogram=new Histogram(argb);
        if (histogram.colorCount==0) {
            return new int[0];
        }
        return medianCut(histogram);
    }

    public void quantize(int[] argb) {
        int[] palette=buildPalette(argb);
        if (palette.length==0) {
            return;
        }
        ColorLut lut=new ColorLut(palette);
        for (int index=0;index<argb.length;index++) {
            if ((argb[index]>>>24)!=0) {
                argb[index]=lut.lookupArgb(argb[index]);
            }
        }
    }

    private int[] medianCut(Histogram histogram) {
        int[] bins=histogram.bins;
        int[] starts=new int[maxColors];
        int[] ends=new int[maxColors];
        int[] palette=new int[maxColors];
        int boxCount=1;
        starts[0]=0;
        ends[0]=histogram.colorCount;
        while (boxCount<maxColors) {
            int selected=-1;
            int selectedAxis=-1;
            int selectedRange=-1;
            long selectedWeight=-1L;
            for (int box=0;box<boxCount;box++) {
                int start=starts[box];
                int end=ends[box];
                if (end-start<2) {
                    continue;
                }
                int axis=widestAxis(bins,start,end);
                int range=axisRange(bins,start,end,axis);
                long weight=boxWeight(bins,start,end,histogram.counts);
                if (range>selectedRange||range==selectedRange&&weight>selectedWeight||range==selectedRange&&weight==selectedWeight&&selected<0) {
                    selected=box;
                    selectedAxis=axis;
                    selectedRange=range;
                    selectedWeight=weight;
                }
            }
            if (selected<0||selectedAxis<0||selectedRange<=0) {
                break;
            }
            int start=starts[selected];
            int end=ends[selected];
            sortBins(bins,start,end,selectedAxis);
            int split=findSplit(bins,start,end,histogram.counts);
            if (split<=start||split>=end) {
                break;
            }
            ends[selected]=split;
            starts[boxCount]=split;
            ends[boxCount]=end;
            boxCount++;
        }
        int paletteCount=boxCount;
        for (int box=0;box<paletteCount;box++) {
            long weight=0L;
            long red=0L;
            long green=0L;
            long blue=0L;
            int start=starts[box];
            int end=ends[box];
            for (int index=start;index<end;index++) {
                int bin=bins[index];
                long count=histogram.counts[bin];
                weight+=count;
                red+=histogram.redSums[bin];
                green+=histogram.greenSums[bin];
                blue+=histogram.blueSums[bin];
            }
            int redChannel=(int)(red/weight);
            int greenChannel=(int)(green/weight);
            int blueChannel=(int)(blue/weight);
            palette[box]=(redChannel<<16)|(greenChannel<<8)|blueChannel;
        }
        int[] result=Arrays.copyOf(palette,paletteCount);
        Arrays.sort(result);
        return result;
    }

    private static int findSplit(int[] bins,int start,int end,int[] counts) {
        long total=0L;
        for (int index=start;index<end;index++) {
            total+=counts[bins[index]];
        }
        long target=(total+1L)>>>1;
        long accumulated=0L;
        for (int index=start;index<end;index++) {
            accumulated+=counts[bins[index]];
            if (accumulated>=target) {
                int split=index+1;
                if (split==start) {
                    return start+1;
                }
                if (split==end) {
                    return end-1;
                }
                return split;
            }
        }
        return end-1;
    }

    private static long boxWeight(int[] bins,int start,int end,int[] counts) {
        long result=0L;
        for (int index=start;index<end;index++) {
            result+=counts[bins[index]];
        }
        return result;
    }

    private static int widestAxis(int[] bins,int start,int end) {
        int minRed=HISTOGRAM_LEVELS;
        int maxRed=0;
        int minGreen=HISTOGRAM_LEVELS;
        int maxGreen=0;
        int minBlue=HISTOGRAM_LEVELS;
        int maxBlue=0;
        for (int index=start;index<end;index++) {
            int bin=bins[index];
            int red=channel(bin,0);
            int green=channel(bin,1);
            int blue=channel(bin,2);
            minRed=Math.min(minRed,red);
            maxRed=Math.max(maxRed,red);
            minGreen=Math.min(minGreen,green);
            maxGreen=Math.max(maxGreen,green);
            minBlue=Math.min(minBlue,blue);
            maxBlue=Math.max(maxBlue,blue);
        }
        int redRange=maxRed-minRed;
        int greenRange=maxGreen-minGreen;
        int blueRange=maxBlue-minBlue;
        if (redRange>=greenRange&&redRange>=blueRange) {
            return 0;
        }
        if (greenRange>=blueRange) {
            return 1;
        }
        return 2;
    }

    private static int axisRange(int[] bins,int start,int end,int axis) {
        int minimum=HISTOGRAM_LEVELS;
        int maximum=0;
        for (int index=start;index<end;index++) {
            int value=channel(bins[index],axis);
            minimum=Math.min(minimum,value);
            maximum=Math.max(maximum,value);
        }
        return maximum-minimum;
    }

    private static int channel(int bin,int axis) {
        int shift=12-axis*6;
        return (bin>>>shift)&(HISTOGRAM_LEVELS-1);
    }

    private static void sortBins(int[] bins,int from,int to,int axis) {
        while (to-from>16) {
            int middle=from+(to-from)/2;
            int first=bins[from];
            int second=bins[middle];
            int third=bins[to-1];
            int pivotBin=medianBin(first,second,third,axis);
            int left=from;
            int right=to-1;
            while (left<=right) {
                while (compare(bins[left],pivotBin,axis)<0) {
                    left++;
                }
                while (compare(bins[right],pivotBin,axis)>0) {
                    right--;
                }
                if (left<=right) {
                    int value=bins[left];
                    bins[left]=bins[right];
                    bins[right]=value;
                    left++;
                    right--;
                }
            }
            int leftSize=right-from+1;
            int rightSize=to-left;
            if (leftSize<rightSize) {
                sortBins(bins,from,right+1,axis);
                from=left;
            } else {
                sortBins(bins,left,to,axis);
                to=right+1;
            }
        }
        for (int index=from+1;index<to;index++) {
            int value=bins[index];
            int position=index-1;
            while (position>=from&&compare(bins[position],value,axis)>0) {
                bins[position+1]=bins[position];
                position--;
            }
            bins[position+1]=value;
        }
    }

    private static int medianBin(int first,int second,int third,int axis) {
        if (compare(first,second,axis)<=0) {
            if (compare(second,third,axis)<=0) {
                return second;
            }
            return compare(first,third,axis)<=0?third:first;
        }
        if (compare(first,third,axis)<=0) {
            return first;
        }
        return compare(second,third,axis)<=0?third:second;
    }

    private static int compare(int first,int second,int axis) {
        int firstChannel=channel(first,axis);
        int secondChannel=channel(second,axis);
        if (firstChannel<secondChannel) {
            return -1;
        }
        if (firstChannel>secondChannel) {
            return 1;
        }
        if (first<second) {
            return -1;
        }
        if (first>second) {
            return 1;
        }
        return 0;
    }

    private static final class Histogram {
        private final int[] counts=new int[HISTOGRAM_SIZE];
        private final long[] redSums=new long[HISTOGRAM_SIZE];
        private final long[] greenSums=new long[HISTOGRAM_SIZE];
        private final long[] blueSums=new long[HISTOGRAM_SIZE];
        private final int[] bins=new int[HISTOGRAM_SIZE];
        private int colorCount;

        private Histogram(int[] argb) {
            for (int index=0;index<argb.length;index++) {
                int color=argb[index];
                if ((color>>>24)==0) {
                    continue;
                }
                int red=(color>>>16)&0xFF;
                int green=(color>>>8)&0xFF;
                int blue=color&0xFF;
                int bin=((red>>>2)<<12)|((green>>>2)<<6)|(blue>>>2);
                counts[bin]++;
                redSums[bin]+=red;
                greenSums[bin]+=green;
                blueSums[bin]+=blue;
            }
            for (int bin=0;bin<HISTOGRAM_SIZE;bin++) {
                if (counts[bin]!=0) {
                    bins[colorCount++]=bin;
                }
            }
        }
    }
}
