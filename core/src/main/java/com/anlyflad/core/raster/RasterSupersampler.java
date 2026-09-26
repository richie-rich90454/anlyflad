package com.anlyflad.core.raster;

import java.util.ArrayList;
import java.util.List;
import com.anlyflad.core.model.VectorPath;

public final class RasterSupersampler {
    public static final int MAX_SCALE=4;
    public static final long MAX_SUPERSAMPLED_PIXELS=1L<<25;
    public static final int MAX_PALETTE_COLORS=256;
    private static final int PALETTE_SLOT_COUNT=512;
    private static final int MATCH_CACHE_SIZE=1<<20;

    private RasterSupersampler() {
    }

    public static int scaleFor(int width,int height) {
        long pixels=(long)width*(long)height;
        if (pixels*(long)(MAX_SCALE*MAX_SCALE)<=MAX_SUPERSAMPLED_PIXELS) {
            return MAX_SCALE;
        }
        if (pixels*4L<=MAX_SUPERSAMPLED_PIXELS) {
            return 2;
        }
        return 1;
    }

    public static RasterFrame sample(RasterFrame frame) {
        if (frame==null) {
            throw new IllegalArgumentException("frame must not be null");
        }
        int scale=scaleFor(frame.getWidth(),frame.getHeight());
        return scale<=1?frame:upsample(frame,scale);
    }

    public static RasterFrame upsample(RasterFrame frame,int scale) {
        if (frame==null) {
            throw new IllegalArgumentException("frame must not be null");
        }
        if (scale<1||scale>MAX_SCALE) {
            throw new IllegalArgumentException("scale must be between 1 and "+MAX_SCALE);
        }
        if (scale==1) {
            return frame;
        }
        int[] palette=collectPalette(frame);
        if (palette==null) {
            return frame;
        }
        int width=frame.getWidth();
        int height=frame.getHeight();
        long targetLength=(long)width*scale*(long)height*scale;
        if (targetLength>(long)Integer.MAX_VALUE) {
            throw new IllegalArgumentException("supersampled frame exceeds the pixel limit");
        }
        int targetWidth=width*scale;
        int targetHeight=height*scale;
        int[] source=frame.getOwnedPixels();
        int[] target=new int[(int)targetLength];
        int[] matches=new int[MATCH_CACHE_SIZE];
        double step=1.0/scale;
        for (int y=0;y<targetHeight;y++) {
            double sourceY=(y+0.5)*step-0.5;
            int top=(int)Math.floor(sourceY);
            double verticalWeight=sourceY-top;
            int topRow=clamp(top,0,height-1)*width;
            int bottomRow=clamp(top+1,0,height-1)*width;
            int targetRow=y*targetWidth;
            for (int x=0;x<targetWidth;x++) {
                double sourceX=(x+0.5)*step-0.5;
                int left=(int)Math.floor(sourceX);
                double horizontalWeight=sourceX-left;
                int leftColumn=clamp(left,0,width-1);
                int rightColumn=clamp(left+1,0,width-1);
                int topLeft=source[topRow+leftColumn];
                int topRight=source[topRow+rightColumn];
                int bottomLeft=source[bottomRow+leftColumn];
                int bottomRight=source[bottomRow+rightColumn];
                if (topLeft==topRight&&topLeft==bottomLeft&&topLeft==bottomRight) {
                    target[targetRow+x]=topLeft;
                    continue;
                }
                double topLeftWeight=(1.0-horizontalWeight)*(1.0-verticalWeight);
                double topRightWeight=horizontalWeight*(1.0-verticalWeight);
                double bottomLeftWeight=(1.0-horizontalWeight)*verticalWeight;
                double bottomRightWeight=horizontalWeight*verticalWeight;
                double alpha=topLeftWeight*alpha(topLeft)+topRightWeight*alpha(topRight)+bottomLeftWeight*alpha(bottomLeft)+bottomRightWeight*alpha(bottomRight);
                if (alpha<=0.5) {
                    target[targetRow+x]=0;
                    continue;
                }
                double red=topLeftWeight*premultiplied(topLeft,16)+topRightWeight*premultiplied(topRight,16)+bottomLeftWeight*premultiplied(bottomLeft,16)+bottomRightWeight*premultiplied(bottomRight,16);
                double green=topLeftWeight*premultiplied(topLeft,8)+topRightWeight*premultiplied(topRight,8)+bottomLeftWeight*premultiplied(bottomLeft,8)+bottomRightWeight*premultiplied(bottomRight,8);
                double blue=topLeftWeight*premultiplied(topLeft,0)+topRightWeight*premultiplied(topRight,0)+bottomLeftWeight*premultiplied(bottomLeft,0)+bottomRightWeight*premultiplied(bottomRight,0);
                int interpolatedAlpha=clamp((int)(alpha+0.5),0,255);
                int interpolatedRed=clamp((int)(red*255.0/alpha+0.5),0,255);
                int interpolatedGreen=clamp((int)(green*255.0/alpha+0.5),0,255);
                int interpolatedBlue=clamp((int)(blue*255.0/alpha+0.5),0,255);
                int key=((interpolatedAlpha>>>3)<<15)|((interpolatedRed>>>3)<<10)|((interpolatedGreen>>>3)<<5)|(interpolatedBlue>>>3);
                int matched=matches[key];
                if (matched==0) {
                    matched=nearestPalette(palette,interpolatedRed,interpolatedGreen,interpolatedBlue,interpolatedAlpha)+1;
                    matches[key]=matched;
                }
                target[targetRow+x]=palette[matched-1];
            }
        }
        return RasterFrame.wrap(targetWidth,targetHeight,target);
    }

    static List<VectorPath> scaleBack(List<VectorPath> paths,int scale) {
        if (scale<=1) {
            return paths;
        }
        double inverse=1.0/scale;
        double inverseArea=inverse*inverse;
        List<VectorPath> scaled=new ArrayList<VectorPath>(paths.size());
        for (int index=0;index<paths.size();index++) {
            VectorPath path=paths.get(index);
            List<double[]> rings=path.getRings();
            for (int ringIndex=0;ringIndex<rings.size();ringIndex++) {
                double[] ring=rings.get(ringIndex);
                for (int coordinate=0;coordinate<ring.length;coordinate++) {
                    ring[coordinate]*=inverse;
                }
            }
            scaled.add(new VectorPath(path.getId(),rings,path.getFill(),path.getOpacity(),path.getFillRule(),path.getArea()*inverseArea));
        }
        return scaled;
    }

    private static int nearestPalette(int[] palette,int red,int green,int blue,int alpha) {
        double premultipliedRed=red*(alpha/255.0);
        double premultipliedGreen=green*(alpha/255.0);
        double premultipliedBlue=blue*(alpha/255.0);
        int best=0;
        double bestDistance=Double.POSITIVE_INFINITY;
        for (int index=0;index<palette.length;index++) {
            int candidate=palette[index];
            double candidateAlpha=candidate>>>24;
            double alphaDelta=alpha-candidateAlpha;
            double redDelta=premultipliedRed-((candidate>>>16)&0xFF)*(candidateAlpha/255.0);
            double greenDelta=premultipliedGreen-((candidate>>>8)&0xFF)*(candidateAlpha/255.0);
            double blueDelta=premultipliedBlue-(candidate&0xFF)*(candidateAlpha/255.0);
            double distance=alphaDelta*alphaDelta+redDelta*redDelta+greenDelta*greenDelta+blueDelta*blueDelta;
            if (distance<bestDistance) {
                bestDistance=distance;
                best=index;
            }
        }
        return best;
    }

    private static int[] collectPalette(RasterFrame frame) {
        int[] pixels=frame.getOwnedPixels();
        int[] colors=new int[MAX_PALETTE_COLORS];
        int[] slots=new int[PALETTE_SLOT_COUNT];
        boolean[] occupied=new boolean[PALETTE_SLOT_COUNT];
        int count=0;
        for (int index=0;index<pixels.length;index++) {
            int color=pixels[index];
            int slot=mix(color)&(PALETTE_SLOT_COUNT-1);
            boolean found=false;
            while (occupied[slot]) {
                if (slots[slot]==color) {
                    found=true;
                    break;
                }
                slot=(slot+1)&(PALETTE_SLOT_COUNT-1);
            }
            if (found) {
                continue;
            }
            if (count==MAX_PALETTE_COLORS) {
                return null;
            }
            occupied[slot]=true;
            slots[slot]=color;
            colors[count++]=color;
        }
        int[] palette=new int[count];
        System.arraycopy(colors,0,palette,0,count);
        return palette;
    }

    private static int mix(int value) {
        int hash=value*0x9E3779B1;
        return hash^(hash>>>16);
    }

    private static int alpha(int color) {
        return color>>>24;
    }

    private static double premultiplied(int color,int shift) {
        return ((color>>>shift)&0xFF)*(color>>>24)/255.0;
    }

    private static int clamp(int value,int minimum,int maximum) {
        return Math.max(minimum,Math.min(maximum,value));
    }
}
