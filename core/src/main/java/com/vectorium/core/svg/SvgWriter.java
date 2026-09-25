package com.vectorium.core.svg;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import com.vectorium.core.model.Color;
import com.vectorium.core.model.VectorDocument;
import com.vectorium.core.model.VectorPath;
public final class SvgWriter {
    public static final int MAX_OUTPUT_BYTES=64*1024*1024;
    private static final char[] HEX={'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};
    public SvgWriter() {
    }
    public static String write(VectorDocument document) {
        if (document==null) {
            throw new IllegalArgumentException("document must not be null");
        }
        if (document.getSourceSvg()!=null) {
            if (utf8Length(document.getSourceSvg())>MAX_OUTPUT_BYTES) {
                throw new IllegalArgumentException("serialized SVG exceeds the 64 MiB output limit");
            }
            return document.getSourceSvg();
        }
        int estimatedSize=128;
        if (document.getPaths().size()<Integer.MAX_VALUE/64) {
            estimatedSize+=document.getPaths().size()*64;
        }
        StringBuilder output=new StringBuilder(estimatedSize);
        output.append("<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"");
        output.append(document.getWidth());
        output.append("\" height=\"");
        output.append(document.getHeight());
        output.append("\" viewBox=\"0 0 ");
        output.append(document.getWidth());
        output.append(' ');
        output.append(document.getHeight());
        output.append('"');
        if (document.getOrigin().isRaster()) {
            output.append(" shape-rendering=\"crispEdges\"");
        }
        output.append(">");
        for (int index=0;index<document.getPaths().size();index++) {
            appendPath(output, document.getPaths().get(index));
            if (output.length()>MAX_OUTPUT_BYTES) {
                throw new IllegalArgumentException("serialized SVG exceeds the 64 MiB output limit");
            }
        }
        output.append("</svg>");
        String serialized=output.toString();
        if (utf8Length(serialized)>MAX_OUTPUT_BYTES) {
            throw new IllegalArgumentException("serialized SVG exceeds the 64 MiB output limit");
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
        return value.getBytes(StandardCharsets.UTF_8).length;
    }
    private static void appendPath(StringBuilder output, VectorPath path) {
        double[] coordinates=path.getCoordinates();
        output.append("<path d=\"M");
        appendNumber(output, coordinates[0]);
        output.append(' ');
        appendNumber(output, coordinates[1]);
        for (int index=2;index<coordinates.length;index+=2) {
            output.append(" L ");
            appendNumber(output, coordinates[index]);
            output.append(' ');
            appendNumber(output, coordinates[index+1]);
        }
        if (path.isClosed()) {
            int last=coordinates.length-2;
            if (coordinates[last]!=coordinates[0]||coordinates[last+1]!=coordinates[1]) {
                output.append(" L ");
                appendNumber(output, coordinates[0]);
                output.append(' ');
                appendNumber(output, coordinates[1]);
            }
            output.append(" Z");
        }
        Color fill=path.getFill();
        output.append("\" fill=\"#");
        appendHex(output, fill.getRed());
        appendHex(output, fill.getGreen());
        appendHex(output, fill.getBlue());
        output.append("\" fill-opacity=\"");
        appendNumber(output, fill.getAlpha()/255.0*path.getOpacity());
        output.append("\"/>");
    }
    private static void appendNumber(StringBuilder output, double value) {
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException("path coordinates must be finite");
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
