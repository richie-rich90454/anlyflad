package com.vectorium.core.geometry;
public final class MarchingSquares {
    private static final int[] EDGE_TABLE=new int[64];
    static {
        for (int index=0;index<EDGE_TABLE.length;index++) {
            EDGE_TABLE[index]=-1;
        }
        EDGE_TABLE[1*4]=3;
        EDGE_TABLE[1*4+1]=0;
        EDGE_TABLE[1*4+2]=-1;
        EDGE_TABLE[1*4+3]=-1;
        EDGE_TABLE[2*4]=0;
        EDGE_TABLE[2*4+1]=1;
        EDGE_TABLE[2*4+2]=-1;
        EDGE_TABLE[2*4+3]=-1;
        EDGE_TABLE[3*4]=3;
        EDGE_TABLE[3*4+1]=1;
        EDGE_TABLE[3*4+2]=-1;
        EDGE_TABLE[3*4+3]=-1;
        EDGE_TABLE[4*4]=1;
        EDGE_TABLE[4*4+1]=2;
        EDGE_TABLE[4*4+2]=-1;
        EDGE_TABLE[4*4+3]=-1;
        EDGE_TABLE[5*4]=0;
        EDGE_TABLE[5*4+1]=1;
        EDGE_TABLE[5*4+2]=3;
        EDGE_TABLE[5*4+3]=2;
        EDGE_TABLE[6*4]=0;
        EDGE_TABLE[6*4+1]=2;
        EDGE_TABLE[6*4+2]=-1;
        EDGE_TABLE[6*4+3]=-1;
        EDGE_TABLE[7*4]=0;
        EDGE_TABLE[7*4+1]=2;
        EDGE_TABLE[7*4+2]=-1;
        EDGE_TABLE[7*4+3]=-1;
        EDGE_TABLE[8*4]=2;
        EDGE_TABLE[8*4+1]=3;
        EDGE_TABLE[8*4+2]=-1;
        EDGE_TABLE[8*4+3]=-1;
        EDGE_TABLE[9*4]=2;
        EDGE_TABLE[9*4+1]=0;
        EDGE_TABLE[9*4+2]=-1;
        EDGE_TABLE[9*4+3]=-1;
        EDGE_TABLE[10*4]=0;
        EDGE_TABLE[10*4+1]=3;
        EDGE_TABLE[10*4+2]=1;
        EDGE_TABLE[10*4+3]=2;
        EDGE_TABLE[11*4]=1;
        EDGE_TABLE[11*4+1]=2;
        EDGE_TABLE[11*4+2]=-1;
        EDGE_TABLE[11*4+3]=-1;
        EDGE_TABLE[12*4]=0;
        EDGE_TABLE[12*4+1]=2;
        EDGE_TABLE[12*4+2]=-1;
        EDGE_TABLE[12*4+3]=-1;
        EDGE_TABLE[13*4]=0;
        EDGE_TABLE[13*4+1]=1;
        EDGE_TABLE[13*4+2]=-1;
        EDGE_TABLE[13*4+3]=-1;
        EDGE_TABLE[14*4]=0;
        EDGE_TABLE[14*4+1]=3;
        EDGE_TABLE[14*4+2]=-1;
        EDGE_TABLE[14*4+3]=-1;
    }
    private MarchingSquares() {
    }
    public static int appendCell(double[] field, int width, int height, int cellX, int cellY, double threshold, double[] output, int outputOffset) {
        if (field==null) {
            throw new IllegalArgumentException("field must not be null");
        }
        if (width<2||height<2) {
            throw new IllegalArgumentException("field dimensions must be positive");
        }
        long fieldLength=(long)width*height;
        if (fieldLength>field.length) {
            throw new IllegalArgumentException("field is smaller than its dimensions");
        }
        if (cellX<0||cellY<0||cellX>=width-1||cellY>=height-1) {
            throw new IllegalArgumentException("cell is outside the field");
        }
        validateFinite(threshold);
        if (output==null||outputOffset<0||outputOffset>output.length) {
            throw new IllegalArgumentException("output offset is invalid");
        }
        int bottomLeftIndex=cellY*width+cellX;
        int bottomRightIndex=bottomLeftIndex+1;
        int topRightIndex=bottomLeftIndex+width;
        int topLeftIndex=topRightIndex+1;
        double bottomLeft=field[bottomLeftIndex];
        double bottomRight=field[bottomRightIndex];
        double topRight=field[topRightIndex];
        double topLeft=field[topLeftIndex];
        validateFinite(bottomLeft);
        validateFinite(bottomRight);
        validateFinite(topRight);
        validateFinite(topLeft);
        int mask=0;
        if (bottomLeft>=threshold) {
            mask|=1;
        }
        if (bottomRight>=threshold) {
            mask|=2;
        }
        if (topRight>=threshold) {
            mask|=4;
        }
        if (topLeft>=threshold) {
            mask|=8;
        }
        if (mask==0||mask==15) {
            return 0;
        }
        int firstEdge=EDGE_TABLE[mask*4];
        int secondEdge=EDGE_TABLE[mask*4+1];
        int thirdEdge=EDGE_TABLE[mask*4+2];
        int fourthEdge=EDGE_TABLE[mask*4+3];
        if (mask==5||mask==10) {
            double center=bottomLeft*0.25+bottomRight*0.25+topRight*0.25+topLeft*0.25;
            validateResult(center);
            boolean centerInside=center>=threshold;
            if (mask==5&&!centerInside) {
                firstEdge=0;
                secondEdge=3;
                thirdEdge=1;
                fourthEdge=2;
            } else if (mask==5) {
                firstEdge=0;
                secondEdge=1;
                thirdEdge=3;
                fourthEdge=2;
            } else if (centerInside) {
                firstEdge=0;
                secondEdge=3;
                thirdEdge=1;
                fourthEdge=2;
            } else {
                firstEdge=0;
                secondEdge=1;
                thirdEdge=3;
                fourthEdge=2;
            }
        }
        int outputCount=thirdEdge<0?4:8;
        if (outputCount>output.length-outputOffset) {
            throw new IllegalArgumentException("output is too small for cell contour");
        }
        double bottomX=cellX+interpolate(bottomLeft, bottomRight, threshold);
        double bottomY=(double)cellY;
        double rightX=(double)(cellX+1);
        double rightY=cellY+interpolate(bottomRight, topRight, threshold);
        double topX=(double)(cellX+1)-interpolate(topRight, topLeft, threshold);
        double topY=(double)(cellY+1);
        double leftX=(double)cellX;
        double leftY=(double)(cellY+1)-interpolate(topLeft, bottomLeft, threshold);
        double firstX=edgeX(firstEdge, bottomX, rightX, topX, leftX);
        double firstY=edgeY(firstEdge, bottomY, rightY, topY, leftY);
        double secondX=edgeX(secondEdge, bottomX, rightX, topX, leftX);
        double secondY=edgeY(secondEdge, bottomY, rightY, topY, leftY);
        validateResult(firstX);
        validateResult(firstY);
        validateResult(secondX);
        validateResult(secondY);
        output[outputOffset]=firstX;
        output[outputOffset+1]=firstY;
        output[outputOffset+2]=secondX;
        output[outputOffset+3]=secondY;
        if (outputCount==8) {
            double thirdX=edgeX(thirdEdge, bottomX, rightX, topX, leftX);
            double thirdY=edgeY(thirdEdge, bottomY, rightY, topY, leftY);
            double fourthX=edgeX(fourthEdge, bottomX, rightX, topX, leftX);
            double fourthY=edgeY(fourthEdge, bottomY, rightY, topY, leftY);
            validateResult(thirdX);
            validateResult(thirdY);
            validateResult(fourthX);
            validateResult(fourthY);
            output[outputOffset+4]=thirdX;
            output[outputOffset+5]=thirdY;
            output[outputOffset+6]=fourthX;
            output[outputOffset+7]=fourthY;
        }
        return outputCount;
    }
    private static double interpolate(double first, double second, double threshold) {
        double denominator=second-first;
        if (denominator==0.0) {
            return 0.5;
        }
        validateResult(denominator);
        double amount=(threshold-first)/denominator;
        validateResult(amount);
        if (amount<0.0) {
            return 0.0;
        }
        if (amount>1.0) {
            return 1.0;
        }
        return amount;
    }
    private static double edgeX(int edge, double bottomX, double rightX, double topX, double leftX) {
        if (edge==0) {
            return bottomX;
        }
        if (edge==1) {
            return rightX;
        }
        if (edge==2) {
            return topX;
        }
        return leftX;
    }
    private static double edgeY(int edge, double bottomY, double rightY, double topY, double leftY) {
        if (edge==0) {
            return bottomY;
        }
        if (edge==1) {
            return rightY;
        }
        if (edge==2) {
            return topY;
        }
        return leftY;
    }
    private static void validateFinite(double value) {
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException("field values must be finite");
        }
    }
    private static void validateResult(double value) {
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException("interpolation result must be finite");
        }
    }
}
