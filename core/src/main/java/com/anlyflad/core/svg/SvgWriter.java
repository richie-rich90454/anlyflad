package com.anlyflad.core.svg;
import java.io.IOException;
import java.io.Writer;
import java.util.List;
import com.anlyflad.core.model.Color;
import com.anlyflad.core.model.VectorDocument;
import com.anlyflad.core.model.VectorPath;
public final class SvgWriter {
    public static final int MAX_OUTPUT_BYTES=1024*1024*1024;
    private static final double COORDINATE_SCALE=100.0;
    private static final char[] HEX={'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};
    public SvgWriter() {
    }
    public static String write(VectorDocument document) {
        if (document==null) {
            throw new IllegalArgumentException("document must not be null");
        }
        if (document.getSourceSvg()!=null) {
            if (utf8Length(document.getSourceSvg())>MAX_OUTPUT_BYTES) {
                throw new IllegalArgumentException("serialized SVG exceeds the 1 GiB output limit");
            }
            return document.getSourceSvg();
        }
        int estimatedSize=128;
        if (document.getPaths().size()<Integer.MAX_VALUE/64) {
            estimatedSize+=document.getPaths().size()*64;
        }
        double outputScale=document.getOutputScale();
        long outputWidth=Math.round(document.getWidth()*outputScale);
        long outputHeight=Math.round(document.getHeight()*outputScale);
        if (outputWidth<1L||outputHeight<1L||outputWidth>Integer.MAX_VALUE||outputHeight>Integer.MAX_VALUE) {
            throw new IllegalArgumentException("scaled output size is outside the supported range");
        }
        StringBuilder output=new StringBuilder(estimatedSize);
        output.append("<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"");
        output.append(outputWidth);
        output.append("\" height=\"");
        output.append(outputHeight);
        output.append("\" viewBox=\"0 0 ");
        output.append(document.getWidth());
        output.append(' ');
        output.append(document.getHeight());
        output.append('"');
        output.append(">");
        boolean raster=document.getOrigin().isRaster();
        for (int index=0;index<document.getPaths().size();index++) {
            VectorPath path=document.getPaths().get(index);
            // Traced regions share subpixel boundaries, so a 1px stroke (half a pixel per side) seals rendering seams without widening exact runs, which stay unsealed.
            boolean seal=raster&&(path.isCompound()||path.hasCubicData());
            appendPath(output, path, seal);
            if (output.length()>MAX_OUTPUT_BYTES) {
                throw new IllegalArgumentException("serialized SVG exceeds the 1 GiB output limit");
            }
        }
        output.append("</svg>");
        String serialized=output.toString();
        if (utf8Length(serialized)>MAX_OUTPUT_BYTES) {
            throw new IllegalArgumentException("serialized SVG exceeds the 1 GiB output limit");
        }
        return serialized;
    }
    public static void write(VectorDocument document, Writer writer) throws IOException {
        if (writer==null) {
            throw new IllegalArgumentException("writer must not be null");
        }
        writer.write(write(document));
    }
    public String serialize(VectorDocument document) {
        return write(document);
    }
    public void serialize(VectorDocument document, Writer writer) throws IOException {
        write(document, writer);
    }
    public static void write(Writer writer, VectorDocument document) throws IOException {
        write(document, writer);
    }
    public static String toSvg(VectorDocument document) {
        return new SvgWriter().write(document);
    }
    public static void writeTo(VectorDocument document, Writer writer) throws IOException {
        new SvgWriter().write(document, writer);
    }
    static int utf8Length(String value) {
        if (value==null) {
            throw new IllegalArgumentException("value must not be null");
        }
        long length=0L;
        for (int index=0;index<value.length();index++) {
            char current=value.charAt(index);
            if (current<=0x7F) {
                length++;
            } else if (current<=0x7FF) {
                length+=2L;
            } else if (Character.isHighSurrogate(current)&&index+1<value.length()&&Character.isLowSurrogate(value.charAt(index+1))) {
                length+=4L;
                index++;
            } else if (Character.isSurrogate(current)) {
                length++;
            } else {
                length+=3L;
            }
            if (length>Integer.MAX_VALUE) {
                throw new IllegalArgumentException("UTF-8 value is too large");
            }
        }
        return (int)length;
    }
    private static void appendPath(StringBuilder output, VectorPath path, boolean seal) {
        List<double[]> rings=path.getRings();
        double[][] cubicRings=path.getCubicRingCoordinates();
        output.append("<path d=\"");
        for (int ringIndex=0;ringIndex<rings.size();ringIndex++) {
            if (ringIndex>0) {
                output.append(' ');
            }
            if (cubicRings.length>ringIndex&&cubicRings[ringIndex]!=null) {
                appendCubicRing(output, cubicRings[ringIndex], path.isClosed());
            } else {
                appendRing(output, rings.get(ringIndex), path.isClosed());
            }
        }
        output.append('"');
        if (path.getFillRule().isEvenOdd()) {
            output.append(" fill-rule=\"evenodd\"");
        }
        Color fill=path.getFill();
        output.append(" fill=\"#");
        appendHex(output, fill.getRed());
        appendHex(output, fill.getGreen());
        appendHex(output, fill.getBlue());
        double opacity=fill.getAlpha()/255.0*path.getOpacity();
        if (opacity<1.0) {
            output.append("\" fill-opacity=\"");
            appendScalar(output, opacity);
        }
        if (seal) {
            output.append("\" stroke=\"#");
            appendHex(output, fill.getRed());
            appendHex(output, fill.getGreen());
            appendHex(output, fill.getBlue());
            if (opacity<1.0) {
                output.append("\" stroke-opacity=\"");
                appendScalar(output, opacity);
            }
            output.append("\" stroke-width=\"1\" stroke-linejoin=\"round");
        }
        output.append("\"/>");
    }
    private static void appendCubicRing(StringBuilder output, double[] segments, boolean closed) {
        if (segments.length<8||segments.length%8!=0) {
            throw new IllegalArgumentException("cubic ring must contain complete cubic segments");
        }
        output.append('M');
        appendNumber(output, segments[0]);
        output.append(' ');
        appendNumber(output, segments[1]);
        for (int offset=0;offset<segments.length;offset+=8) {
            output.append('C');
            appendNumber(output, segments[offset+2]);
            output.append(' ');
            appendNumber(output, segments[offset+3]);
            output.append(' ');
            appendNumber(output, segments[offset+4]);
            output.append(' ');
            appendNumber(output, segments[offset+5]);
            output.append(' ');
            appendNumber(output, segments[offset+6]);
            output.append(' ');
            appendNumber(output, segments[offset+7]);
        }
        if (closed) {
            int last=segments.length-2;
            if (segments[last]!=segments[0]||segments[last+1]!=segments[1]) {
                throw new IllegalArgumentException("closed cubic ring must end at its start");
            }
            output.append(" Z");
        }
    }
    private static void appendRing(StringBuilder output, double[] coordinates, boolean closed) {
        if (closed&&isAxisAlignedRectangle(coordinates)) {
            appendRectangle(output, coordinates);
            return;
        }
        output.append('M');
        appendNumber(output, coordinates[0]);
        output.append(' ');
        appendNumber(output, coordinates[1]);
        for (int index=2;index<coordinates.length;index+=2) {
            output.append('L');
            appendNumber(output, coordinates[index]);
            output.append(' ');
            appendNumber(output, coordinates[index+1]);
        }
        if (closed) {
            int last=coordinates.length-2;
            if (coordinates[last]!=coordinates[0]||coordinates[last+1]!=coordinates[1]) {
                output.append(" L ");
                appendNumber(output, coordinates[0]);
                output.append(' ');
                appendNumber(output, coordinates[1]);
            }
            output.append(" Z");
        }
    }
    private static boolean isAxisAlignedRectangle(double[] coordinates) {
        if (coordinates.length!=8) {
            return false;
        }
        double minimumX=coordinates[0];
        double maximumX=coordinates[0];
        double minimumY=coordinates[1];
        double maximumY=coordinates[1];
        for (int index=2;index<8;index+=2) {
            minimumX=Math.min(minimumX,coordinates[index]);
            maximumX=Math.max(maximumX,coordinates[index]);
            minimumY=Math.min(minimumY,coordinates[index+1]);
            maximumY=Math.max(maximumY,coordinates[index+1]);
        }
        if (minimumX==maximumX||minimumY==maximumY) {
            return false;
        }
        for (int index=0;index<8;index+=2) {
            double x=coordinates[index];
            double y=coordinates[index+1];
            if ((x!=minimumX&&x!=maximumX)||(y!=minimumY&&y!=maximumY)) {
                return false;
            }
        }
        for (int index=0;index<4;index++) {
            int next=(index+1)%4;
            double x=coordinates[index*2];
            double y=coordinates[index*2+1];
            double nextX=coordinates[next*2];
            double nextY=coordinates[next*2+1];
            if (x!=nextX&&y!=nextY) {
                return false;
            }
        }
        return true;
    }
    private static void appendRectangle(StringBuilder output, double[] coordinates) {
        double minimumX=coordinates[0];
        double maximumX=coordinates[0];
        double minimumY=coordinates[1];
        double maximumY=coordinates[1];
        for (int index=2;index<8;index+=2) {
            minimumX=Math.min(minimumX,coordinates[index]);
            maximumX=Math.max(maximumX,coordinates[index]);
            minimumY=Math.min(minimumY,coordinates[index+1]);
            maximumY=Math.max(maximumY,coordinates[index+1]);
        }
        double width=maximumX-minimumX;
        double height=maximumY-minimumY;
        output.append('M');
        appendNumber(output,minimumX);
        output.append(' ');
        appendNumber(output,minimumY);
        output.append('h');
        appendNumber(output,width);
        output.append('v');
        appendNumber(output,height);
        output.append('h');
        appendNumber(output,-width);
        output.append('z');
    }
    private static void appendNumber(StringBuilder output, double value) {
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException("path coordinates must be finite");
        }
        double rounded=Math.rint(value*COORDINATE_SCALE)/COORDINATE_SCALE;
        if (rounded==0.0) {
            output.append('0');
            return;
        }
        if (rounded==Math.rint(rounded)&&Math.abs(rounded)<=9007199254740992.0) {
            output.append((long)rounded);
            return;
        }
        output.append(rounded);
    }
    private static void appendScalar(StringBuilder output, double value) {
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException("opacity must be finite");
        }
        if (value==0.0) {
            output.append('0');
            return;
        }
        if (value==Math.rint(value)&&Math.abs(value)<=9007199254740992.0) {
            output.append((long)value);
            return;
        }
        output.append(value);
    }
    private static void appendHex(StringBuilder output, int value) {
        output.append(HEX[(value>>>4)&0x0F]);
        output.append(HEX[value&0x0F]);
    }
}
