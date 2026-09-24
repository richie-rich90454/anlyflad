package com.vectorium.core.svg;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import com.vectorium.core.model.Color;
import com.vectorium.core.model.PathId;
import com.vectorium.core.model.SvgOrigin;
import com.vectorium.core.model.VectorDocument;
import com.vectorium.core.model.VectorPath;
public final class SvgParser {
    private static final int MAX_CONTEXT_DEPTH=128;
    private static final int MAX_ELEMENT_DEPTH=512;
    private static final int MAX_ATTRIBUTES=64;
    private static final int ROOT=1;
    private static final int GROUP=2;
    private static final int OTHER=3;
    private static final int IGNORED=4;
    public SvgParser() {
    }
    public VectorDocument parse(String sourceName, String svg) throws SvgParseException {
        if (sourceName==null||sourceName.trim().isEmpty()) {
            throw new SvgParseException("sourceName must not be blank");
        }
        if (svg==null) {
            throw new SvgParseException("SVG source must not be null");
        }
        ParseState state=new ParseState(svg);
        scan(state);
        return createDocument(sourceName, state);
    }
    public VectorDocument parse(Reader reader, String sourceName) throws SvgParseException {
        if (reader==null) {
            throw new SvgParseException("reader must not be null");
        }
        StringBuilder source=new StringBuilder();
        char[] buffer=new char[4096];
        try {
            int count=reader.read(buffer);
            while (count>=0) {
                if (count>0) {
                    source.append(buffer, 0, count);
                }
                count=reader.read(buffer);
            }
        } catch (IOException exception) {
            throw new SvgParseException("unable to read SVG source", exception);
        }
        return parse(sourceName, source.toString());
    }
    public VectorDocument parse(String sourceName, Reader reader) throws SvgParseException {
        return parse(reader, sourceName);
    }
    private void scan(ParseState state) throws SvgParseException {
        while (state.position<state.source.length()) {
            char value=state.source.charAt(state.position);
            if (value=='<') {
                if (startsWith(state, "<!--")) {
                    skipComment(state);
                } else if (startsWith(state, "<![CDATA[")) {
                    skipCdata(state);
                } else if (startsWith(state, "<!DOCTYPE")) {
                    skipDoctype(state);
                } else if (startsWith(state, "<?")) {
                    skipProcessingInstruction(state);
                } else if (startsWith(state, "</")) {
                    scanEndTag(state);
                } else if (state.position+1<state.source.length()&&isNameStart(state.source.charAt(state.position+1))) {
                    scanStartTag(state);
                } else {
                    throw error("invalid markup at position "+state.position);
                }
            } else {
                scanText(state);
            }
        }
        if (!state.rootSeen) {
            throw error("SVG root element is missing");
        }
        if (!state.rootClosed) {
            throw error("SVG root element is not closed");
        }
    }
    private void scanText(ParseState state) throws SvgParseException {
        int start=state.position;
        while (state.position<state.source.length()&&state.source.charAt(state.position)!='<') {
            state.position++;
        }
        String text=state.source.substring(start, state.position);
        decodeEntities(text);
        if (!state.rootSeen&&!isWhitespace(text)) {
            throw error("content is not allowed before the SVG root");
        }
        if (state.rootClosed&&!isWhitespace(text)) {
            throw error("content is not allowed after the SVG root");
        }
    }
    private void scanStartTag(ParseState state) throws SvgParseException {
        Tag tag=readTag(state);
        if (!state.rootSeen) {
            if (!tag.name.equals("svg")) {
                throw error("expected svg root element");
            }
            state.rootSeen=true;
            initializeRoot(state, tag);
            if (tag.selfClosing) {
                state.rootClosed=true;
            } else {
                state.elementTypes[0]=ROOT;
                state.elementDepth=1;
            }
            return;
        }
        if (state.rootClosed) {
            throw error("only one SVG root element is allowed");
        }
        String name=tag.name;
        if (state.skipDepth>0||isIgnored(name)) {
            if (!tag.selfClosing) {
                state.skipDepth++;
                pushElement(state, IGNORED);
            }
            return;
        }
        if (name.equals("g")||name.equals("svg")) {
            if (!tag.selfClosing) {
                pushContext(state, tag);
                pushElement(state, GROUP);
            }
            return;
        }
        if (isShape(name)) {
            parseShape(state, name, tag);
            if (!tag.selfClosing) {
                pushElement(state, OTHER);
            }
            return;
        }
        if (!tag.selfClosing) {
            pushElement(state, OTHER);
        }
    }
    private void scanEndTag(ParseState state) throws SvgParseException {
        if (!state.rootSeen||state.rootClosed||state.elementDepth==0) {
            throw error("unexpected closing element");
        }
        int nameStart=state.position+2;
        int index=nameStart;
        while (index<state.source.length()&&isNameCharacter(state.source.charAt(index))) {
            index++;
        }
        if (nameStart==index) {
            throw error("closing element name is missing");
        }
        String name=localName(state.source.substring(nameStart, index));
        while (index<state.source.length()&&isWhitespace(state.source.charAt(index))) {
            index++;
        }
        if (index==state.source.length()||state.source.charAt(index)!='>') {
            throw error("malformed closing element");
        }
        state.position=index+1;
        int type=state.elementTypes[state.elementDepth-1];
        if (type==ROOT) {
            if (!name.equals("svg")) {
                throw error("closing element does not match svg root");
            }
            state.elementDepth--;
            state.rootClosed=true;
            if (state.elementDepth!=0||state.contextDepth!=1) {
                throw error("unclosed SVG group");
            }
        } else if (type==GROUP) {
            if (!name.equals("g")&&!name.equals("svg")) {
                throw error("closing element does not match group");
            }
            state.contextDepth--;
            state.elementDepth--;
        } else if (type==IGNORED) {
            if (state.skipDepth>0) {
                state.skipDepth--;
            }
            state.elementDepth--;
        } else {
            state.elementDepth--;
        }
    }
    private void initializeRoot(ParseState state, Tag tag) throws SvgParseException {
        for (int index=0;index<4;index++) {
            state.fillStack[index]=0;
        }
        state.fillStack[3]=255;
        for (int index=0;index<3;index++) {
            state.colorStack[index]=0;
        }
        state.opacityStack[0]=1.0;
        state.fillOpacityStack[0]=1.0;
        state.matrixStack[0]=1.0;
        state.matrixStack[1]=0.0;
        state.matrixStack[2]=0.0;
        state.matrixStack[3]=1.0;
        state.matrixStack[4]=0.0;
        state.matrixStack[5]=0.0;
        state.contextDepth=1;
        state.width=0.0;
        state.height=0.0;
        state.viewBoxWidth=0.0;
        state.viewBoxHeight=0.0;
        state.widthSet=false;
        state.heightSet=false;
        state.widthPercent=false;
        state.heightPercent=false;
        state.viewBoxSet=false;
        String width=tag.get("width");
        if (width!=null) {
            state.width=parseLength(width, "width");
            if (state.width<0.0) {
                throw error("width must not be negative");
            }
            state.widthPercent=width.trim().endsWith("%");
            state.widthSet=true;
        }
        String height=tag.get("height");
        if (height!=null) {
            state.height=parseLength(height, "height");
            if (state.height<0.0) {
                throw error("height must not be negative");
            }
            state.heightPercent=height.trim().endsWith("%");
            state.heightSet=true;
        }
        String viewBox=tag.get("viewBox");
        if (viewBox!=null) {
            double[] values=parseNumbers(viewBox, 4, "viewBox");
            if (values[0]==Double.NaN) {
                throw error("invalid viewBox");
            }
            if (values[2]<0.0||values[3]<0.0) {
                throw error("viewBox dimensions must not be negative");
            }
            state.viewBoxWidth=values[2];
            state.viewBoxHeight=values[3];
            state.viewBoxSet=true;
        }
        if (state.widthPercent&&state.viewBoxSet) {
            state.width*=state.viewBoxWidth;
        }
        if (state.heightPercent&&state.viewBoxSet) {
            state.height*=state.viewBoxHeight;
        }
        applyAttributes(state, tag, 0, -1);
    }
    private void pushContext(ParseState state, Tag tag) throws SvgParseException {
        if (state.contextDepth>=MAX_CONTEXT_DEPTH) {
            throw error("SVG group nesting is too deep");
        }
        int parent=state.contextDepth-1;
        int target=state.contextDepth;
        copyContext(state, target, parent);
        state.contextDepth++;
        applyAttributes(state, tag, target, parent);
    }
    private void copyContext(ParseState state, int target, int parent) {
        int matrixOffset=target*6;
        int parentOffset=parent*6;
        for (int index=0;index<6;index++) {
            state.matrixStack[matrixOffset+index]=state.matrixStack[parentOffset+index];
        }
        int fillOffset=target*4;
        int parentFillOffset=parent*4;
        for (int index=0;index<4;index++) {
            state.fillStack[fillOffset+index]=state.fillStack[parentFillOffset+index];
        }
        int colorOffset=target*3;
        int parentColorOffset=parent*3;
        for (int index=0;index<3;index++) {
            state.colorStack[colorOffset+index]=state.colorStack[parentColorOffset+index];
        }
        state.opacityStack[target]=state.opacityStack[parent];
        state.fillOpacityStack[target]=state.fillOpacityStack[parent];
    }
    private void applyAttributes(ParseState state, Tag tag, int target, int parent) throws SvgParseException {
        String color=tag.get("color");
        if (color!=null) {
            applyColor(state, target, color);
        }
        String fill=tag.get("fill");
        if (fill!=null) {
            applyFill(state, target, parent, fill);
        }
        String opacity=tag.get("opacity");
        if (opacity!=null) {
            state.opacityStack[target]=baseOpacity(state, parent)*parseOpacity(opacity, "opacity");
        }
        String fillOpacity=tag.get("fill-opacity");
        if (fillOpacity!=null) {
            state.fillOpacityStack[target]=baseFillOpacity(state, parent)*parseOpacity(fillOpacity, "fill-opacity");
        }
        String transform=tag.get("transform");
        if (transform!=null) {
            applyTransform(state, target, parent, transform);
        }
        String style=tag.get("style");
        if (style!=null) {
            applyStyle(state, target, parent, style);
        }
    }
    private void applyStyle(ParseState state, int target, int parent, String style) throws SvgParseException {
        String[] names=new String[32];
        String[] values=new String[32];
        int count=0;
        int position=0;
        while (position<style.length()) {
            int semicolon=style.indexOf(';', position);
            int end=semicolon<0?style.length():semicolon;
            String declaration=style.substring(position, end).trim();
            position=semicolon<0?style.length():semicolon+1;
            if (declaration.length()==0) {
                continue;
            }
            int colon=declaration.indexOf(':');
            if (colon<=0||colon==declaration.length()-1||count==names.length) {
                throw error("invalid style declaration");
            }
            names[count]=declaration.substring(0, colon).trim().toLowerCase(Locale.ENGLISH);
            values[count]=declaration.substring(colon+1).trim();
            count++;
        }
        for (int index=0;index<count;index++) {
            if (names[index].equals("color")) {
                applyColor(state, target, values[index]);
            }
        }
        for (int index=0;index<count;index++) {
            String name=names[index];
            String value=values[index];
            if (name.equals("color")) {
                continue;
            }
            if (name.equals("fill")) {
                applyFill(state, target, parent, value);
            } else if (name.equals("opacity")) {
                state.opacityStack[target]=baseOpacity(state, parent)*parseOpacity(value, "opacity");
            } else if (name.equals("fill-opacity")) {
                state.fillOpacityStack[target]=baseFillOpacity(state, parent)*parseOpacity(value, "fill-opacity");
            } else if (name.equals("transform")) {
                applyTransform(state, target, parent, value);
            }
        }
    }
    private void applyColor(ParseState state, int target, String value) throws SvgParseException {
        String color=value.trim();
        if (color.equalsIgnoreCase("currentColor")) {
            throw error("color cannot be currentColor");
        }
        Color parsed=parseColor(color);
        int offset=target*3;
        state.colorStack[offset]=parsed.getRed();
        state.colorStack[offset+1]=parsed.getGreen();
        state.colorStack[offset+2]=parsed.getBlue();
    }
    private void applyFill(ParseState state, int target, int parent, String value) throws SvgParseException {
        String fill=value.trim();
        int offset=target*4;
        if (fill.equalsIgnoreCase("inherit")) {
            if (parent<0) {
                state.fillStack[offset]=0;
                state.fillStack[offset+1]=0;
                state.fillStack[offset+2]=0;
                state.fillStack[offset+3]=255;
            } else {
                int parentOffset=parent*4;
                for (int index=0;index<4;index++) {
                    state.fillStack[offset+index]=state.fillStack[parentOffset+index];
                }
            }
            return;
        }
        if (fill.equalsIgnoreCase("none")||fill.equalsIgnoreCase("transparent")) {
            state.fillStack[offset]=0;
            state.fillStack[offset+1]=0;
            state.fillStack[offset+2]=0;
            state.fillStack[offset+3]=0;
            return;
        }
        Color parsed;
        if (fill.equalsIgnoreCase("currentColor")) {
            int colorOffset=target*3;
            parsed=new Color(state.colorStack[colorOffset], state.colorStack[colorOffset+1], state.colorStack[colorOffset+2], 255);
        } else {
            parsed=parseColor(fill);
        }
        state.fillStack[offset]=parsed.getRed();
        state.fillStack[offset+1]=parsed.getGreen();
        state.fillStack[offset+2]=parsed.getBlue();
        state.fillStack[offset+3]=parsed.getAlpha();
    }
    private void applyTransform(ParseState state, int target, int parent, String value) throws SvgParseException {
        SvgTransform parentTransform=parent<0?SvgTransform.identity():matrix(state, parent);
        SvgTransform ownTransform;
        if (value.trim().equalsIgnoreCase("none")) {
            ownTransform=SvgTransform.identity();
        } else {
            ownTransform=SvgTransform.parse(value);
        }
        double[] composed=parentTransform.concatenate(ownTransform).toArray();
        int offset=target*6;
        for (int index=0;index<6;index++) {
            state.matrixStack[offset+index]=composed[index];
        }
    }
    private double baseOpacity(ParseState state, int parent) {
        return parent<0?1.0:state.opacityStack[parent];
    }
    private double baseFillOpacity(ParseState state, int parent) {
        return parent<0?1.0:state.fillOpacityStack[parent];
    }
    private SvgTransform matrix(ParseState state, int context) {
        int offset=context*6;
        return new SvgTransform(state.matrixStack[offset], state.matrixStack[offset+1], state.matrixStack[offset+2], state.matrixStack[offset+3], state.matrixStack[offset+4], state.matrixStack[offset+5]);
    }
    private void parseShape(ParseState state, String name, Tag tag) throws SvgParseException {
        int context=state.contextDepth;
        if (context>=MAX_CONTEXT_DEPTH) {
            throw error("SVG context nesting is too deep");
        }
        copyContext(state, context, state.contextDepth-1);
        applyAttributes(state, tag, context, state.contextDepth-1);
        SvgTransform transform=matrix(state, context);
        int fillOffset=context*4;
        Color fill=new Color(state.fillStack[fillOffset], state.fillStack[fillOffset+1], state.fillStack[fillOffset+2], state.fillStack[fillOffset+3]);
        double opacity=state.opacityStack[context]*state.fillOpacityStack[context];
        StringBuilder path=new StringBuilder(64);
        if (name.equals("path")) {
            String data=tag.get("d");
            if (data==null) {
                throw error("path is missing d");
            }
            path.append(data);
        } else if (name.equals("rect")) {
            double x=attributeNumber(tag, "x", 0.0);
            double y=attributeNumber(tag, "y", 0.0);
            double width=requiredNonNegative(tag, "width");
            double height=requiredNonNegative(tag, "height");
            appendRect(path, x, y, width, height);
        } else if (name.equals("circle")) {
            double cx=attributeNumber(tag, "cx", 0.0);
            double cy=attributeNumber(tag, "cy", 0.0);
            double radius=requiredNonNegative(tag, "r");
            appendCircle(path, cx, cy, radius);
        } else if (name.equals("ellipse")) {
            double cx=attributeNumber(tag, "cx", 0.0);
            double cy=attributeNumber(tag, "cy", 0.0);
            double radiusX=requiredNonNegative(tag, "rx");
            double radiusY=requiredNonNegative(tag, "ry");
            appendEllipse(path, cx, cy, radiusX, radiusY);
        } else if (name.equals("line")) {
            appendLine(path, requiredNumber(tag, "x1"), requiredNumber(tag, "y1"), requiredNumber(tag, "x2"), requiredNumber(tag, "y2"));
        } else if (name.equals("polygon")) {
            appendPolygon(path, requiredPoints(tag, "points"), true);
        } else {
            appendPolygon(path, requiredPoints(tag, "points"), false);
        }
        SvgPathParser pathParser=new SvgPathParser();
        List<VectorPath> parsed=pathParser.parse(path.toString(), transform, fill, opacity);
        state.paths.addAll(parsed);
    }
    private double requiredNumber(Tag tag, String name) throws SvgParseException {
        String value=tag.get(name);
        if (value==null) {
            throw error("missing "+name);
        }
        return parseLength(value, name);
    }
    private double attributeNumber(Tag tag, String name, double defaultValue) throws SvgParseException {
        String value=tag.get(name);
        if (value==null) {
            return defaultValue;
        }
        return parseLength(value, name);
    }
    private double requiredNonNegative(Tag tag, String name) throws SvgParseException {
        double value=requiredNumber(tag, name);
        if (value<0.0) {
            throw error(name+" must not be negative");
        }
        return value;
    }
    private double[] requiredPoints(Tag tag, String name) throws SvgParseException {
        String value=tag.get(name);
        if (value==null) {
            throw error("missing "+name);
        }
        double[] points=parseNumbers(value, -1, name);
        if (points.length==0||points.length%2!=0) {
            throw error(name+" must contain coordinate pairs");
        }
        return points;
    }
    private VectorDocument createDocument(String sourceName, ParseState state) throws SvgParseException {
        double width=state.width;
        double height=state.height;
        if (!state.widthSet) {
            width=state.viewBoxSet?state.viewBoxWidth:derivedWidth(state);
        }
        if (!state.heightSet) {
            height=state.viewBoxSet?state.viewBoxHeight:derivedHeight(state);
        }
        int pixelWidth=toDimension(width, state.widthSet||state.viewBoxSet);
        int pixelHeight=toDimension(height, state.heightSet||state.viewBoxSet);
        List<VectorPath> paths=new ArrayList<VectorPath>(state.paths.size());
        for (int index=0;index<state.paths.size();index++) {
            VectorPath path=state.paths.get(index);
            paths.add(new VectorPath(PathId.of(index), path.getCoordinates(), path.isClosed(), path.getFill(), path.getOpacity()));
        }
        long pixelLength=(long)pixelWidth*(long)pixelHeight;
        if (pixelLength>Integer.MAX_VALUE) {
            throw error("SVG dimensions are too large");
        }
        try {
            return new VectorDocument(sourceName, new SvgOrigin(sourceName), paths, pixelWidth, pixelHeight, new int[(int)pixelLength]);
        } catch (IllegalArgumentException exception) {
            throw new SvgParseException("unable to create SVG document: "+exception.getMessage(), exception);
        }
    }
    private int toDimension(double value, boolean explicit) throws SvgParseException {
        if (!Double.isFinite(value)||value<0.0||value>Integer.MAX_VALUE) {
            throw error("SVG dimensions must be finite and nonnegative");
        }
        int dimension=(int)Math.ceil(value);
        if (dimension<0) {
            throw error("SVG dimension is too small");
        }
        if (!explicit&&dimension==0) {
            return 1;
        }
        return dimension;
    }
    private double derivedWidth(ParseState state) {
        double maximum=0.0;
        for (int index=0;index<state.paths.size();index++) {
            double[] coordinates=state.paths.get(index).getCoordinates();
            for (int coordinate=0;coordinate<coordinates.length;coordinate+=2) {
                maximum=Math.max(maximum, coordinates[coordinate]);
            }
        }
        return maximum;
    }
    private double derivedHeight(ParseState state) {
        double maximum=0.0;
        for (int index=0;index<state.paths.size();index++) {
            double[] coordinates=state.paths.get(index).getCoordinates();
            for (int coordinate=1;coordinate<coordinates.length;coordinate+=2) {
                maximum=Math.max(maximum, coordinates[coordinate]);
            }
        }
        return maximum;
    }
    private void pushElement(ParseState state, int type) throws SvgParseException {
        if (state.elementDepth>=MAX_ELEMENT_DEPTH) {
            throw error("SVG element nesting is too deep");
        }
        state.elementTypes[state.elementDepth]=type;
        state.elementDepth++;
    }
    private Tag readTag(ParseState state) throws SvgParseException {
        int start=state.position+1;
        int index=start;
        while (index<state.source.length()&&isNameCharacter(state.source.charAt(index))) {
            index++;
        }
        if (start==index) {
            throw error("element name is missing");
        }
        String rawName=state.source.substring(start, index);
        String name=localName(rawName);
        Tag tag=new Tag(name);
        while (true) {
            while (index<state.source.length()&&isWhitespace(state.source.charAt(index))) {
                index++;
            }
            if (index==state.source.length()) {
                throw error("unterminated element tag");
            }
            char value=state.source.charAt(index);
            if (value=='>') {
                state.position=index+1;
                return tag;
            }
            if (value=='/'&&index+1<state.source.length()&&state.source.charAt(index+1)=='>') {
                tag.selfClosing=true;
                state.position=index+2;
                return tag;
            }
            if (value=='/'||value=='='||value=='<') {
                throw error("malformed element attribute");
            }
            int nameStart=index;
            while (index<state.source.length()&&!isWhitespace(state.source.charAt(index))&&state.source.charAt(index)!='='&&state.source.charAt(index)!='/'&&state.source.charAt(index)!='>') {
                index++;
            }
            if (nameStart==index) {
                throw error("attribute name is missing");
            }
            String attributeName=state.source.substring(nameStart, index);
            while (index<state.source.length()&&isWhitespace(state.source.charAt(index))) {
                index++;
            }
            if (index==state.source.length()||state.source.charAt(index)!='=') {
                throw error("attribute is missing =");
            }
            index++;
            while (index<state.source.length()&&isWhitespace(state.source.charAt(index))) {
                index++;
            }
            if (index==state.source.length()||(state.source.charAt(index)!='\''&&state.source.charAt(index)!='"')) {
                throw error("attribute value must be quoted");
            }
            char quote=state.source.charAt(index++);
            int valueStart=index;
            while (index<state.source.length()&&state.source.charAt(index)!=quote) {
                if (state.source.charAt(index)=='<') {
                    throw error("attribute value contains <");
                }
                index++;
            }
            if (index==state.source.length()) {
                throw error("unterminated attribute value");
            }
            String attributeValue=decodeEntities(state.source.substring(valueStart, index));
            index++;
            if (tag.has(attributeName)) {
                throw error("duplicate attribute "+attributeName);
            }
            tag.add(attributeName, attributeValue);
        }
    }
    private void skipComment(ParseState state) throws SvgParseException {
        int end=state.source.indexOf("-->", state.position+4);
        if (end<0) {
            throw error("unterminated comment");
        }
        state.position=end+3;
    }
    private void skipCdata(ParseState state) throws SvgParseException {
        int end=state.source.indexOf("]]>", state.position+9);
        if (end<0) {
            throw error("unterminated CDATA section");
        }
        state.position=end+3;
    }
    private void skipProcessingInstruction(ParseState state) throws SvgParseException {
        int end=state.source.indexOf("?>", state.position+2);
        if (end<0) {
            throw error("unterminated processing instruction");
        }
        state.position=end+2;
    }
    private void skipDoctype(ParseState state) throws SvgParseException {
        int index=state.position+9;
        int subset=0;
        char quote=0;
        while (index<state.source.length()) {
            char value=state.source.charAt(index);
            if (quote!=0) {
                if (value==quote) {
                    quote=0;
                }
            } else if (value=='\''||value=='"') {
                quote=value;
            } else if (value=='[') {
                subset++;
            } else if (value==']') {
                if (subset>0) {
                    subset--;
                }
            } else if (value=='>'&&subset==0) {
                state.position=index+1;
                return;
            }
            index++;
        }
        throw error("unterminated doctype");
    }
    private String decodeEntities(String value) throws SvgParseException {
        int ampersand=value.indexOf('&');
        if (ampersand<0) {
            return value;
        }
        StringBuilder result=new StringBuilder(value.length());
        int position=0;
        while (position<value.length()) {
            char current=value.charAt(position);
            if (current!='&') {
                result.append(current);
                position++;
                continue;
            }
            int end=value.indexOf(';', position+1);
            if (end<0||end-position>12) {
                throw error("malformed XML entity");
            }
            String entity=value.substring(position+1, end);
            int code;
            if (entity.equals("amp")) {
                code='&';
            } else if (entity.equals("lt")) {
                code='<';
            } else if (entity.equals("gt")) {
                code='>';
            } else if (entity.equals("quot")) {
                code='"';
            } else if (entity.equals("apos")) {
                code='\'';
            } else if (entity.length()>1&&entity.charAt(0)=='#') {
                int numberStart=entity.charAt(1)=='x'||entity.charAt(1)=='X'?2:1;
                if (numberStart==entity.length()) {
                    throw error("malformed numeric XML entity");
                }
                int radix=numberStart==2?16:10;
                long numericCode=0;
                for (int index=numberStart;index<entity.length();index++) {
                    int digit=Character.digit(entity.charAt(index), radix);
                    if (digit<0) {
                        throw error("malformed numeric XML entity");
                    }
                    numericCode=numericCode*radix+digit;
                    if (numericCode>0x10FFFF) {
                        throw error("numeric XML entity is out of range");
                    }
                }
                code=(int)numericCode;
            } else {
                throw error("unsupported XML entity "+entity);
            }
            appendCodePoint(result, code);
            position=end+1;
        }
        return result.toString();
    }
    private void appendCodePoint(StringBuilder result, int code) throws SvgParseException {
        if (code<0||code>0x10FFFF||(code>=0xD800&&code<=0xDFFF)) {
            throw error("invalid XML code point");
        }
        if (code<=0xFFFF) {
            result.append((char)code);
            return;
        }
        int adjusted=code-0x10000;
        result.append((char)(0xD800+(adjusted>>>10)));
        result.append((char)(0xDC00+(adjusted&0x3FF)));
    }
    private double parseLength(String value, String name) throws SvgParseException {
        String trimmed=value.trim();
        if (trimmed.length()==0) {
            throw error(name+" is empty");
        }
        int[] cursor=new int[]{0};
        int end=readNumberEnd(trimmed, cursor[0]);
        if (end==cursor[0]) {
            throw error(name+" must be numeric");
        }
        double number=parseNumber(trimmed, cursor[0], end, name);
        int unitEnd=end;
        while (unitEnd<trimmed.length()&&!isWhitespace(trimmed.charAt(unitEnd))) {
            unitEnd++;
        }
        String unit=trimmed.substring(end, unitEnd).toLowerCase(Locale.ENGLISH);
        int suffixEnd=unitEnd;
        while (suffixEnd<trimmed.length()&&isWhitespace(trimmed.charAt(suffixEnd))) {
            suffixEnd++;
        }
        if (suffixEnd!=trimmed.length()) {
            throw error("malformed "+name);
        }
        if (unit.length()>0&&!unit.equals("px")&&!unit.equals("pt")&&!unit.equals("pc")&&!unit.equals("cm")&&!unit.equals("mm")&&!unit.equals("in")&&!unit.equals("em")&&!unit.equals("ex")&&!unit.equals("%")) {
            throw error("unsupported unit in "+name);
        }
        if (unit.equals("%")) {
            number/=100.0;
        }
        if (!Double.isFinite(number)) {
            throw error(name+" must be finite");
        }
        return number;
    }
    private double parseOpacity(String value, String name) throws SvgParseException {
        double number=parseLength(value, name);
        if (number<0.0||number>1.0) {
            throw error(name+" must be between zero and one");
        }
        return number;
    }
    private Color parseColor(String value) throws SvgParseException {
        String color=value.trim();
        if (color.length()==0) {
            throw error("color is empty");
        }
        if (color.equalsIgnoreCase("black")) {
            return new Color(0, 0, 0, 255);
        }
        if (color.equalsIgnoreCase("white")) {
            return new Color(255, 255, 255, 255);
        }
        if (color.equalsIgnoreCase("red")) {
            return new Color(255, 0, 0, 255);
        }
        if (color.equalsIgnoreCase("green")) {
            return new Color(0, 128, 0, 255);
        }
        if (color.equalsIgnoreCase("blue")) {
            return new Color(0, 0, 255, 255);
        }
        if (color.equalsIgnoreCase("yellow")) {
            return new Color(255, 255, 0, 255);
        }
        if (color.equalsIgnoreCase("cyan")) {
            return new Color(0, 255, 255, 255);
        }
        if (color.equalsIgnoreCase("magenta")) {
            return new Color(255, 0, 255, 255);
        }
        if (color.equalsIgnoreCase("gray")||color.equalsIgnoreCase("grey")) {
            return new Color(128, 128, 128, 255);
        }
        if (color.equalsIgnoreCase("orange")) {
            return new Color(255, 165, 0, 255);
        }
        if (color.equalsIgnoreCase("purple")) {
            return new Color(128, 0, 128, 255);
        }
        if (color.equalsIgnoreCase("transparent")) {
            return new Color(0, 0, 0, 0);
        }
        if (color.equalsIgnoreCase("currentColor")) {
            throw error("currentColor is not a concrete color");
        }
        if (color.charAt(0)=='#') {
            try {
                return Color.fromHex(color);
            } catch (IllegalArgumentException exception) {
                throw new SvgParseException("invalid color "+color, exception);
            }
        }
        if (color.regionMatches(true, 0, "rgb(", 0, 4)||color.regionMatches(true, 0, "rgba(", 0, 5)) {
            return parseRgbColor(color);
        }
        throw error("unsupported color "+color);
    }
    private Color parseRgbColor(String value) throws SvgParseException {
        int open=value.indexOf('(');
        int close=value.lastIndexOf(')');
        if (open<0||close!=value.length()-1) {
            throw error("invalid rgb color");
        }
        double[] values=parseNumbers(value.substring(open+1, close), -1, "rgb color");
        if (values.length!=3&&values.length!=4) {
            throw error("rgb color expects three or four components");
        }
        int red=colorComponent(values[0], "red");
        int green=colorComponent(values[1], "green");
        int blue=colorComponent(values[2], "blue");
        int alpha=values.length==4?opacityComponent(values[3]):255;
        return new Color(red, green, blue, alpha);
    }
    private int colorComponent(double value, String name) throws SvgParseException {
        if (value<0.0||value>255.0||!Double.isFinite(value)) {
            throw error(name+" color component is out of range");
        }
        return (int)Math.round(value);
    }
    private int opacityComponent(double value) throws SvgParseException {
        if (value<0.0||value>1.0||!Double.isFinite(value)) {
            throw error("rgb alpha is out of range");
        }
        return (int)Math.round(value*255.0);
    }
    private double[] parseNumbers(String value, int required, String name) throws SvgParseException {
        int[] cursor=new int[]{0};
        double[] values=new double[16];
        int count=0;
        while (true) {
            skipNumberWhitespace(value, cursor);
            if (cursor[0]==value.length()) {
                break;
            }
            if (value.charAt(cursor[0])==',') {
                throw error("unexpected comma in "+name);
            }
            int end=readNumberEnd(value, cursor[0]);
            if (end==cursor[0]) {
                throw error("malformed number in "+name);
            }
            if (count==values.length) {
                values=Arrays.copyOf(values, values.length*2);
            }
            values[count]=parseNumber(value, cursor[0], end, name);
            count++;
            cursor[0]=end;
        }
        if (required>=0&&count!=required) {
            throw error(name+" expects "+required+" numbers");
        }
        double[] result=new double[count];
        System.arraycopy(values, 0, result, 0, count);
        return result;
    }
    private double parseNumber(String value, int start, int end, String name) throws SvgParseException {
        try {
            double number=Double.parseDouble(value.substring(start, end));
            if (!Double.isFinite(number)) {
                throw error(name+" must be finite");
            }
            return number;
        } catch (NumberFormatException exception) {
            throw new SvgParseException("malformed number in "+name, exception);
        }
    }
    private void skipNumberWhitespace(String value, int[] cursor) {
        while (cursor[0]<value.length()&&isWhitespace(value.charAt(cursor[0]))) {
            cursor[0]++;
        }
        if (cursor[0]<value.length()&&value.charAt(cursor[0])==',') {
            cursor[0]++;
            while (cursor[0]<value.length()&&isWhitespace(value.charAt(cursor[0]))) {
                cursor[0]++;
            }
        }
    }
    private int readNumberEnd(String value, int start) {
        int index=start;
        if (index<value.length()&&(value.charAt(index)=='+'||value.charAt(index)=='-')) {
            index++;
        }
        int digits=0;
        while (index<value.length()&&isDigit(value.charAt(index))) {
            index++;
            digits++;
        }
        if (index<value.length()&&value.charAt(index)=='.') {
            index++;
            while (index<value.length()&&isDigit(value.charAt(index))) {
                index++;
                digits++;
            }
        }
        if (digits==0) {
            return start;
        }
        if (index<value.length()&&(value.charAt(index)=='e'||value.charAt(index)=='E')) {
            int exponentStart=index;
            index++;
            if (index<value.length()&&(value.charAt(index)=='+'||value.charAt(index)=='-')) {
                index++;
            }
            int exponentDigits=0;
            while (index<value.length()&&isDigit(value.charAt(index))) {
                index++;
                exponentDigits++;
            }
            if (exponentDigits==0) {
                return exponentStart;
            }
        }
        return index;
    }
    private void appendRect(StringBuilder path, double x, double y, double width, double height) {
        path.append('M');
        path.append(x);
        path.append(' ');
        path.append(y);
        path.append('L');
        path.append(x+width);
        path.append(' ');
        path.append(y);
        path.append('L');
        path.append(x+width);
        path.append(' ');
        path.append(y+height);
        path.append('L');
        path.append(x);
        path.append(' ');
        path.append(y+height);
        path.append('Z');
    }
    private void appendCircle(StringBuilder path, double cx, double cy, double radius) {
        appendEllipse(path, cx, cy, radius, radius);
    }
    private void appendEllipse(StringBuilder path, double cx, double cy, double radiusX, double radiusY) {
        double constant=0.5522847498307933;
        path.append('M');
        path.append(cx+radiusX);
        path.append(' ');
        path.append(cy);
        path.append('C');
        appendPoint(path, cx+radiusX, cy+constant*radiusY);
        appendPoint(path, cx+constant*radiusX, cy+radiusY);
        appendPoint(path, cx, cy+radiusY);
        path.append('C');
        appendPoint(path, cx-constant*radiusX, cy+radiusY);
        appendPoint(path, cx-radiusX, cy+constant*radiusY);
        appendPoint(path, cx-radiusX, cy);
        path.append('C');
        appendPoint(path, cx-radiusX, cy-constant*radiusY);
        appendPoint(path, cx-constant*radiusX, cy-radiusY);
        appendPoint(path, cx, cy-radiusY);
        path.append('C');
        appendPoint(path, cx+constant*radiusX, cy-radiusY);
        appendPoint(path, cx+radiusX, cy-constant*radiusY);
        appendPoint(path, cx+radiusX, cy);
        path.append('Z');
    }
    private void appendLine(StringBuilder path, double x1, double y1, double x2, double y2) {
        path.append('M');
        appendPoint(path, x1, y1);
        path.append('L');
        appendPoint(path, x2, y2);
    }
    private void appendPolygon(StringBuilder path, double[] points, boolean closed) {
        path.append('M');
        appendPoint(path, points[0], points[1]);
        for (int index=2;index<points.length;index+=2) {
            path.append('L');
            appendPoint(path, points[index], points[index+1]);
        }
        if (closed) {
            path.append('Z');
        }
    }
    private void appendPoint(StringBuilder path, double x, double y) {
        path.append(' ');
        path.append(x);
        path.append(' ');
        path.append(y);
    }
    private static boolean startsWith(ParseState state, String value) {
        return state.source.regionMatches(state.position, value, 0, value.length());
    }
    private String localName(String value) {
        int colon=value.indexOf(':');
        return colon<0?value:value.substring(colon+1);
    }
    private static boolean isIgnored(String name) {
        return name.equals("defs")||name.equals("metadata")||name.equals("title")||name.equals("desc")||name.equals("style")||name.equals("symbol")||name.equals("clipPath")||name.equals("mask")||name.equals("pattern")||name.equals("marker")||name.equals("linearGradient")||name.equals("radialGradient")||name.equals("filter");
    }
    private static boolean isShape(String name) {
        return name.equals("path")||name.equals("rect")||name.equals("circle")||name.equals("ellipse")||name.equals("line")||name.equals("polygon")||name.equals("polyline");
    }
    private static boolean isNameStart(char value) {
        return (value>='a'&&value<='z')||(value>='A'&&value<='Z')||value=='_'||value==':';
    }
    private static boolean isNameCharacter(char value) {
        return isNameStart(value)||isDigit(value)||value=='-'||value=='.';
    }
    private static boolean isDigit(char value) {
        return value>='0'&&value<='9';
    }
    private static boolean isWhitespace(char value) {
        return value==' '||value=='\t'||value=='\r'||value=='\n'||value=='\f';
    }
    private static boolean isWhitespace(String value) {
        for (int index=0;index<value.length();index++) {
            if (!isWhitespace(value.charAt(index))) {
                return false;
            }
        }
        return true;
    }
    private static SvgParseException error(String message) {
        return new SvgParseException(message);
    }
    private static final class Tag {
        private final String name;
        private final String[] names=new String[MAX_ATTRIBUTES];
        private final String[] values=new String[MAX_ATTRIBUTES];
        private int count;
        private boolean selfClosing;
        private Tag(String name) {
            this.name=name;
        }
        private String get(String attributeName) {
            for (int index=0;index<count;index++) {
                if (names[index].equals(attributeName)) {
                    return values[index];
                }
            }
            return null;
        }
        private boolean has(String attributeName) {
            return get(attributeName)!=null;
        }
        private void add(String attributeName, String attributeValue) throws SvgParseException {
            if (count==names.length) {
                throw error("too many element attributes");
            }
            names[count]=attributeName;
            values[count]=attributeValue;
            count++;
        }
    }
    private static final class ParseState {
        private final String source;
        private final List<VectorPath> paths=new ArrayList<VectorPath>();
        private final int[] elementTypes=new int[MAX_ELEMENT_DEPTH];
        private final double[] matrixStack=new double[MAX_CONTEXT_DEPTH*6];
        private final int[] fillStack=new int[MAX_CONTEXT_DEPTH*4];
        private final int[] colorStack=new int[MAX_CONTEXT_DEPTH*3];
        private final double[] opacityStack=new double[MAX_CONTEXT_DEPTH];
        private final double[] fillOpacityStack=new double[MAX_CONTEXT_DEPTH];
        private int position;
        private int elementDepth;
        private int contextDepth;
        private int skipDepth;
        private boolean rootSeen;
        private boolean rootClosed;
        private double width;
        private double height;
        private double viewBoxWidth;
        private double viewBoxHeight;
        private boolean widthSet;
        private boolean heightSet;
        private boolean widthPercent;
        private boolean heightPercent;
        private boolean viewBoxSet;
        private ParseState(String source) {
            this.source=source;
            if (source.length()>0&&source.charAt(0)=='\uFEFF') {
                position=1;
            }
        }
    }
}
