package com.vectorium.core.svg;
import java.util.ArrayList;
import java.util.List;
import com.vectorium.core.geometry.BezierFlattener;
import com.vectorium.core.model.Color;
import com.vectorium.core.model.PathId;
import com.vectorium.core.model.VectorPath;
public final class SvgPathParser {
    private static final int MAX_FLATTEN_STEPS=32;
    private static final int MIN_FLATTEN_STEPS=4;
    private static final int MAX_POINTS=1000000;
    private static final double ARC_TWO_PI=2.0*Math.PI;
    private static final double ARC_PI_OVER_TWO=Math.PI/2.0;
    private static final double ARC_FOUR_THIRDS=4.0/3.0;
    private String data;
    private int position;
    private SvgTransform transform;
    private Color fill;
    private double opacity;
    private VectorPath.FillRule fillRule;
    private BezierFlattener flattener;
    private List<VectorPath> paths;
    private List<PathState> compoundStates;
    private boolean compoundMode;
    private PathState state;
    private int nextPathId;
    public SvgPathParser() {
    }
    public List<VectorPath> parse(String pathData) throws SvgParseException {
        return parse(pathData, SvgTransform.identity(), new Color(0, 0, 0, 255), 1.0, VectorPath.FillRule.NONZERO);
    }
    public List<VectorPath> parse(String pathData, Color fill, double opacity) throws SvgParseException {
        return parse(pathData, SvgTransform.identity(), fill, opacity, VectorPath.FillRule.NONZERO);
    }
    public List<VectorPath> parse(String pathData, SvgTransform transform, Color fill, double opacity) throws SvgParseException {
        return parse(pathData, transform, fill, opacity, VectorPath.FillRule.NONZERO);
    }
    public List<VectorPath> parse(String pathData, SvgTransform transform, Color fill, double opacity, VectorPath.FillRule fillRule) throws SvgParseException {
        return parseInternal(pathData, transform, fill, opacity, fillRule, false);
    }
    public List<VectorPath> parse(String pathData, Color fill, double opacity, SvgTransform transform) throws SvgParseException {
        return parse(pathData, transform, fill, opacity);
    }
    public VectorPath parseCompound(String pathData, SvgTransform transform, Color fill, double opacity, VectorPath.FillRule fillRule) throws SvgParseException {
        return parseInternal(pathData, transform, fill, opacity, fillRule, true).get(0);
    }
    public VectorPath parseCompound(String pathData, Color fill, double opacity) throws SvgParseException {
        return parseCompound(pathData, SvgTransform.identity(), fill, opacity, VectorPath.FillRule.NONZERO);
    }
    public static List<VectorPath> parsePath(String pathData, SvgTransform transform, Color fill, double opacity) throws SvgParseException {
        return new SvgPathParser().parse(pathData, transform, fill, opacity);
    }
    public static List<VectorPath> parsePath(String pathData, Color fill, double opacity) throws SvgParseException {
        return new SvgPathParser().parse(pathData, fill, opacity);
    }
    private List<VectorPath> parseInternal(String pathData, SvgTransform transform, Color fill, double opacity, VectorPath.FillRule fillRule, boolean compoundMode) throws SvgParseException {
        if (pathData==null) {
            throw new SvgParseException("path data must not be null");
        }
        if (transform==null) {
            throw new SvgParseException("path transform must not be null");
        }
        if (fill==null) {
            throw new SvgParseException("path fill must not be null");
        }
        if (fillRule==null) {
            throw new SvgParseException("path fill rule must not be null");
        }
        if (!Double.isFinite(opacity)||opacity<0.0||opacity>1.0) {
            throw new SvgParseException("path opacity must be between zero and one");
        }
        data=pathData;
        position=0;
        this.transform=transform;
        this.fill=fill;
        this.opacity=opacity;
        this.fillRule=fillRule;
        this.compoundMode=compoundMode;
        flattener=new BezierFlattener(MAX_FLATTEN_STEPS);
        paths=new ArrayList<VectorPath>();
        compoundStates=new ArrayList<PathState>();
        state=null;
        nextPathId=0;
        try {
            parseData();
            if (compoundMode) {
                if (compoundStates.isEmpty()) {
                    throw error("path data must contain a moveto");
                }
                paths.add(createCompoundPath());
            }
        } catch (SvgParseException exception) {
            throw exception;
        } catch (IllegalArgumentException exception) {
            throw new SvgParseException("invalid path geometry: "+exception.getMessage(), exception);
        }
        return paths;
    }
    private void parseData() throws SvgParseException {
        char activeCommand=0;
        boolean activeRelative=false;
        skipWhitespace();
        while (position<data.length()) {
            char value=data.charAt(position);
            if (isAsciiLetter(value)) {
                if (!isSupportedCommand(value)) {
                    throw error("unsupported path command "+value);
                }
                position++;
                activeRelative=isRelative(value);
                if (isCloseCommand(value)) {
                    handleClose();
                    activeCommand=0;
                    activeRelative=false;
                } else {
                    activeCommand=toUpperCommand(value);
                    if (activeCommand=='M') {
                        handleMove(activeRelative);
                        activeCommand=activeRelative?'l':'L';
                    } else {
                        handleSegment(activeCommand, activeRelative);
                    }
                }
            } else {
                if (activeCommand==0) {
                    throw error("path data must start with a command");
                }
                handleSegment(activeCommand, activeRelative);
            }
            skipWhitespace();
        }
        finishState();
    }
    private void handleMove(boolean relative) throws SvgParseException {
        double baseX=state==null?0.0:state.currentX;
        double baseY=state==null?0.0:state.currentY;
        double x=readNumber();
        double y=readNumber();
        if (relative) {
            x+=baseX;
            y+=baseY;
        }
        finishState();
        state=new PathState(x, y, transform);
        state.builder.add(x, y);
    }
    private void handleSegment(char command, boolean relative) throws SvgParseException {
        if (state==null) {
            throw error("drawing command must follow a moveto");
        }
        double x=state.currentX;
        double y=state.currentY;
        if (command=='L') {
            double nextX=readNumber();
            double nextY=readNumber();
            if (relative) {
                nextX+=x;
                nextY+=y;
            }
            state.addLine(nextX, nextY);
            clearSmoothState();
        } else if (command=='H') {
            double nextX=readNumber();
            if (relative) {
                nextX+=x;
            }
            state.addLine(nextX, y);
            clearSmoothState();
        } else if (command=='V') {
            double nextY=readNumber();
            if (relative) {
                nextY+=y;
            }
            state.addLine(x, nextY);
            clearSmoothState();
        } else if (command=='C') {
            double control1X=readNumber();
            double control1Y=readNumber();
            double control2X=readNumber();
            double control2Y=readNumber();
            double endX=readNumber();
            double endY=readNumber();
            if (relative) {
                control1X+=x;
                control1Y+=y;
                control2X+=x;
                control2Y+=y;
                endX+=x;
                endY+=y;
            }
            appendCubic(x, y, control1X, control1Y, control2X, control2Y, endX, endY);
            state.setCubic(control2X, control2Y, endX, endY);
        } else if (command=='S') {
            double control1X=state.hasCubic?2.0*x-state.cubicControlX:x;
            double control1Y=state.hasCubic?2.0*y-state.cubicControlY:y;
            double control2X=readNumber();
            double control2Y=readNumber();
            double endX=readNumber();
            double endY=readNumber();
            if (relative) {
                control2X+=x;
                control2Y+=y;
                endX+=x;
                endY+=y;
            }
            appendCubic(x, y, control1X, control1Y, control2X, control2Y, endX, endY);
            state.setCubic(control2X, control2Y, endX, endY);
        } else if (command=='Q') {
            double controlX=readNumber();
            double controlY=readNumber();
            double endX=readNumber();
            double endY=readNumber();
            if (relative) {
                controlX+=x;
                controlY+=y;
                endX+=x;
                endY+=y;
            }
            appendQuadratic(x, y, controlX, controlY, endX, endY);
            state.setQuadratic(controlX, controlY, endX, endY);
        } else if (command=='T') {
            double controlX=state.hasQuadratic?2.0*x-state.quadraticControlX:x;
            double controlY=state.hasQuadratic?2.0*y-state.quadraticControlY:y;
            double endX=readNumber();
            double endY=readNumber();
            if (relative) {
                endX+=x;
                endY+=y;
            }
            appendQuadratic(x, y, controlX, controlY, endX, endY);
            state.setQuadratic(controlX, controlY, endX, endY);
        } else if (command=='A') {
            double radiusX=readNumber();
            double radiusY=readNumber();
            double rotation=readNumber();
            double largeArc=readFlag();
            double sweep=readFlag();
            double endX=readNumber();
            double endY=readNumber();
            if (relative) {
                endX+=x;
                endY+=y;
            }
            appendArc(x, y, radiusX, radiusY, rotation, largeArc, sweep, endX, endY);
            state.currentX=endX;
            state.currentY=endY;
            clearSmoothState();
        } else {
            throw error("unsupported drawing command "+command);
        }
    }
    private void handleClose() throws SvgParseException {
        if (state==null) {
            throw error("close command must follow a moveto");
        }
        state.close();
        clearSmoothState();
    }
    private void appendCubic(double startX, double startY, double control1X, double control1Y, double control2X, double control2Y, double endX, double endY) throws SvgParseException {
        double length=distance(startX, startY, control1X, control1Y)+distance(control1X, control1Y, control2X, control2Y)+distance(control2X, control2Y, endX, endY);
        int steps=flattenSteps(length);
        try {
            state.addCubicGeometry(startX, startY, control1X, control1Y, control2X, control2Y, endX, endY);
            int pointCount=flattener.flatten(startX, startY, control1X, control1Y, control2X, control2Y, endX, endY, steps);
            double[] output=flattener.getOutputCoordinates();
            for (int index=1;index<pointCount;index++) {
                state.builder.add(output[index*2], output[index*2+1]);
            }
        } catch (IllegalArgumentException exception) {
            throw new SvgParseException("invalid cubic curve", exception);
        }
    }
    private void appendQuadratic(double startX, double startY, double controlX, double controlY, double endX, double endY) throws SvgParseException {
        double control1X=startX+(2.0/3.0)*(controlX-startX);
        double control1Y=startY+(2.0/3.0)*(controlY-startY);
        double control2X=endX+(2.0/3.0)*(controlX-endX);
        double control2Y=endY+(2.0/3.0)*(controlY-endY);
        appendCubic(startX, startY, control1X, control1Y, control2X, control2Y, endX, endY);
    }
    private void appendArc(double startX, double startY, double radiusX, double radiusY, double rotation, double largeArc, double sweep, double endX, double endY) throws SvgParseException {
        if (radiusX<0.0||radiusY<0.0) {
            throw error("arc radii must not be negative");
        }
        if ((largeArc!=0.0&&largeArc!=1.0)||(sweep!=0.0&&sweep!=1.0)) {
            throw error("arc flags must be zero or one");
        }
        if (radiusX==0.0||radiusY==0.0) {
            state.addLine(endX, endY);
            return;
        }
        if (startX==endX&&startY==endY) {
            return;
        }
        double radians=Math.toRadians(rotation);
        double cosine=Math.cos(radians);
        double sine=Math.sin(radians);
        double halfX=(startX-endX)*0.5;
        double halfY=(startY-endY)*0.5;
        double transformedX=cosine*halfX+sine*halfY;
        double transformedY=-sine*halfX+cosine*halfY;
        double lambda=transformedX*transformedX/(radiusX*radiusX)+transformedY*transformedY/(radiusY*radiusY);
        if (lambda>1.0) {
            double scale=Math.sqrt(lambda);
            radiusX*=scale;
            radiusY*=scale;
        }
        double radiusXSquared=radiusX*radiusX;
        double radiusYSquared=radiusY*radiusY;
        double numerator=radiusXSquared*radiusYSquared-radiusXSquared*transformedY*transformedY-radiusYSquared*transformedX*transformedX;
        double denominator=radiusXSquared*transformedY*transformedY+radiusYSquared*transformedX*transformedX;
        double factor=0.0;
        if (numerator>0.0&&denominator>0.0) {
            factor=(largeArc==sweep?-1.0:1.0)*Math.sqrt(numerator/denominator);
        }
        double centerXPrime=factor*radiusX*transformedY/radiusY;
        double centerYPrime=-factor*radiusY*transformedX/radiusX;
        double centerX=cosine*centerXPrime-sine*centerYPrime+(startX+endX)*0.5;
        double centerY=sine*centerXPrime+cosine*centerYPrime+(startY+endY)*0.5;
        double startVectorX=(transformedX-centerXPrime)/radiusX;
        double startVectorY=(transformedY-centerYPrime)/radiusY;
        double endVectorX=(-transformedX-centerXPrime)/radiusX;
        double endVectorY=(-transformedY-centerYPrime)/radiusY;
        double startAngle=angle(1.0, 0.0, startVectorX, startVectorY);
        double delta=angle(startVectorX, startVectorY, endVectorX, endVectorY);
        if (sweep==0.0&&delta>0.0) {
            delta-=ARC_TWO_PI;
        }
        if (sweep==1.0&&delta<0.0) {
            delta+=ARC_TWO_PI;
        }
        int segmentCount=(int)Math.ceil(Math.abs(delta)/ARC_PI_OVER_TWO);
        if (segmentCount<1) {
            segmentCount=1;
        }
        double segmentDelta=delta/segmentCount;
        double angleStart=startAngle;
        double currentX=state.currentX;
        double currentY=state.currentY;
        for (int segment=0;segment<segmentCount;segment++) {
            double angleEnd=angleStart+segmentDelta;
            double cosStart=Math.cos(angleStart);
            double sinStart=Math.sin(angleStart);
            double cosEnd=Math.cos(angleEnd);
            double sinEnd=Math.sin(angleEnd);
            double point1X=centerX+cosine*radiusX*cosStart-sine*radiusY*sinStart;
            double point1Y=centerY+sine*radiusX*cosStart+cosine*radiusY*sinStart;
            double point2X=centerX+cosine*radiusX*cosEnd-sine*radiusY*sinEnd;
            double point2Y=centerY+sine*radiusX*cosEnd+cosine*radiusY*sinEnd;
            double derivative1X=-radiusX*cosine*sinStart-radiusY*sine*cosStart;
            double derivative1Y=-radiusX*sine*sinStart+radiusY*cosine*cosStart;
            double derivative2X=-radiusX*cosine*sinEnd-radiusY*sine*cosEnd;
            double derivative2Y=-radiusX*sine*sinEnd+radiusY*cosine*cosEnd;
            double alpha=ARC_FOUR_THIRDS*Math.tan(segmentDelta*0.25);
            double nextX=segment+1==segmentCount?endX:point2X;
            double nextY=segment+1==segmentCount?endY:point2Y;
            appendCubic(currentX, currentY, point1X+alpha*derivative1X, point1Y+alpha*derivative1Y, point2X-alpha*derivative2X, point2Y-alpha*derivative2Y, nextX, nextY);
            currentX=nextX;
            currentY=nextY;
            angleStart=angleEnd;
        }
    }
    private void finishState() {
        if (state==null||state.builder.getPointCount()==0) {
            return;
        }
        if (compoundMode) {
            compoundStates.add(state);
        } else {
            paths.add(createPath(state, PathId.of(nextPathId)));
            nextPathId++;
        }
        state=null;
    }
    private VectorPath createPath(PathState source, PathId id) {
        double[] cubic=source.getCubicCoordinates();
        if (cubic==null) {
            return new VectorPath(id, source.builder.getCoordinates(), source.isClosed(), fill, opacity, fillRule);
        }
        return new VectorPath(id, source.builder.getCoordinates(), cubic, source.isClosed(), fill, opacity, fillRule);
    }
    private VectorPath createCompoundPath() throws SvgParseException {
        if (compoundStates.size()==1) {
            return createPath(compoundStates.get(0), PathId.zero());
        }
        List<double[]> rings=new ArrayList<double[]>(compoundStates.size());
        double[][] cubicRings=new double[compoundStates.size()][];
        boolean hasCubic=false;
        for (int index=0;index<compoundStates.size();index++) {
            PathState source=compoundStates.get(index);
            source.ensureClosed();
            double[] coordinates=source.builder.getCoordinates();
            if (coordinates.length<6) {
                throw error("compound subpath must contain at least three coordinate pairs");
            }
            rings.add(coordinates);
            cubicRings[index]=source.getCubicCoordinates();
            hasCubic|=cubicRings[index]!=null;
        }
        return new VectorPath(PathId.zero(), rings, hasCubic?cubicRings:null, fillRule, fill, opacity);
    }
    private void clearSmoothState() {
        state.hasCubic=false;
        state.hasQuadratic=false;
    }
    private double readNumber() throws SvgParseException {
        skipSeparators();
        int start=position;
        if (position<data.length()&&(data.charAt(position)=='+'||data.charAt(position)=='-')) {
            position++;
        }
        int digits=0;
        while (position<data.length()&&isDigit(data.charAt(position))) {
            position++;
            digits++;
        }
        if (position<data.length()&&data.charAt(position)=='.') {
            position++;
            while (position<data.length()&&isDigit(data.charAt(position))) {
                position++;
                digits++;
            }
        }
        if (digits==0) {
            throw error("expected path number at position "+start);
        }
        if (position<data.length()&&(data.charAt(position)=='e'||data.charAt(position)=='E')) {
            int exponentStart=position;
            position++;
            if (position<data.length()&&(data.charAt(position)=='+'||data.charAt(position)=='-')) {
                position++;
            }
            int exponentDigits=0;
            while (position<data.length()&&isDigit(data.charAt(position))) {
                position++;
                exponentDigits++;
            }
            if (exponentDigits==0) {
                position=exponentStart;
                throw error("malformed path exponent at position "+exponentStart);
            }
        }
        double value;
        try {
            value=Double.parseDouble(data.substring(start, position));
        } catch (NumberFormatException exception) {
            throw new SvgParseException("malformed path number at position "+start, exception);
        }
        if (!Double.isFinite(value)) {
            throw error("path number must be finite at position "+start);
        }
        return value;
    }
    private double readFlag() throws SvgParseException {
        skipSeparators();
        if (position>=data.length()||(data.charAt(position)!='0'&&data.charAt(position)!='1')) {
            throw error("expected arc flag at position "+position);
        }
        return data.charAt(position++)-'0';
    }
    private void skipSeparators() {
        skipWhitespace();
        if (position<data.length()&&data.charAt(position)==',') {
            position++;
            skipWhitespace();
            if (position<data.length()&&data.charAt(position)==',') {
                throw new IllegalArgumentException("path numbers contain repeated commas");
            }
        }
    }
    private void skipWhitespace() {
        while (position<data.length()&&isWhitespace(data.charAt(position))) {
            position++;
        }
    }
    private static int flattenSteps(double length) {
        if (!Double.isFinite(length)||length/4.0>=MAX_FLATTEN_STEPS) {
            return MAX_FLATTEN_STEPS;
        }
        int steps=(int)Math.ceil(length/4.0);
        if (steps<MIN_FLATTEN_STEPS) {
            return MIN_FLATTEN_STEPS;
        }
        if (steps>MAX_FLATTEN_STEPS) {
            return MAX_FLATTEN_STEPS;
        }
        return steps;
    }
    private static double distance(double firstX, double firstY, double secondX, double secondY) {
        double deltaX=secondX-firstX;
        double deltaY=secondY-firstY;
        return Math.sqrt(deltaX*deltaX+deltaY*deltaY);
    }
    private static double angle(double startX, double startY, double endX, double endY) {
        double dot=startX*endX+startY*endY;
        double lengths=Math.sqrt((startX*startX+startY*startY)*(endX*endX+endY*endY));
        if (lengths==0.0) {
            return 0.0;
        }
        double value=dot/lengths;
        if (value>1.0) {
            value=1.0;
        }
        if (value<-1.0) {
            value=-1.0;
        }
        double result=Math.acos(value);
        if (startX*endY-startY*endX<0.0) {
            result=-result;
        }
        return result;
    }
    private static boolean isWhitespace(char value) {
        return value==' '||value=='\t'||value=='\r'||value=='\n'||value=='\f';
    }
    private static boolean isDigit(char value) {
        return value>='0'&&value<='9';
    }
    private static boolean isAsciiLetter(char value) {
        return (value>='a'&&value<='z')||(value>='A'&&value<='Z');
    }
    private static boolean isSupportedCommand(char value) {
        return value=='M'||value=='m'||value=='L'||value=='l'||value=='H'||value=='h'||value=='V'||value=='v'||value=='C'||value=='c'||value=='S'||value=='s'||value=='Q'||value=='q'||value=='T'||value=='t'||value=='A'||value=='a'||value=='Z'||value=='z';
    }
    private static boolean isCloseCommand(char value) {
        return value=='Z'||value=='z';
    }
    private static boolean isRelative(char value) {
        return value>='a'&&value<='z';
    }
    private static char toUpperCommand(char value) {
        if (value>='a'&&value<='z') {
            return (char)(value-'a'+'A');
        }
        return value;
    }
    private static SvgParseException error(String message) {
        return new SvgParseException(message);
    }
    private static final class PathState {
        private final PathBuilder builder;
        private final CubicBuilder cubicBuilder;
        private double startX;
        private double startY;
        private double currentX;
        private double currentY;
        private boolean hasCubic;
        private boolean hasQuadratic;
        private double cubicControlX;
        private double cubicControlY;
        private double quadraticControlX;
        private double quadraticControlY;
        private PathState(double startX, double startY, SvgTransform transform) {
            this.startX=startX;
            this.startY=startY;
            this.currentX=startX;
            this.currentY=startY;
            builder=new PathBuilder(transform);
            cubicBuilder=new CubicBuilder(transform);
        }
        private void addLine(double x, double y) {
            builder.add(x, y);
            builder.closed=false;
            if (cubicBuilder.hasCurve()) {
                cubicBuilder.addLine(currentX, currentY, x, y);
            }
            currentX=x;
            currentY=y;
        }
        private void addCubicGeometry(double startX, double startY, double control1X, double control1Y, double control2X, double control2Y, double endX, double endY) {
            cubicBuilder.startFromFallback(builder);
            cubicBuilder.addCubic(startX, startY, control1X, control1Y, control2X, control2Y, endX, endY);
            builder.closed=false;
            currentX=endX;
            currentY=endY;
        }
        private void setCubic(double controlX, double controlY, double endX, double endY) {
            hasCubic=true;
            hasQuadratic=false;
            cubicControlX=controlX;
            cubicControlY=controlY;
            currentX=endX;
            currentY=endY;
        }
        private void setQuadratic(double controlX, double controlY, double endX, double endY) {
            hasQuadratic=true;
            hasCubic=false;
            quadraticControlX=controlX;
            quadraticControlY=controlY;
            currentX=endX;
            currentY=endY;
        }
        private void close() {
            if (cubicBuilder.hasCurve()&&(currentX!=startX||currentY!=startY)) {
                addLine(startX, startY);
            }
            builder.close(startX, startY);
            currentX=startX;
            currentY=startY;
            hasCubic=false;
            hasQuadratic=false;
        }
        private void ensureClosed() {
            if (!builder.closed) {
                close();
            }
        }
        private boolean isClosed() {
            return builder.closed;
        }
        private double[] getCubicCoordinates() {
            return cubicBuilder.hasCurve()?cubicBuilder.getCoordinates():null;
        }
    }
    private static final class PathBuilder {
        private final SvgTransform transform;
        private double[] coordinates=new double[16];
        private int pointCount;
        private boolean closed;
        private double lastX;
        private double lastY;
        private PathBuilder(SvgTransform transform) {
            this.transform=transform;
        }
        private void add(double x, double y) {
            if (pointCount>=MAX_POINTS) {
                throw new IllegalArgumentException("path point count exceeds the supported limit");
            }
            if (!Double.isFinite(x)||!Double.isFinite(y)) {
                throw new IllegalArgumentException("path coordinates must be finite");
            }
            double transformedX=transform.transformX(x, y);
            double transformedY=transform.transformY(x, y);
            if (!Double.isFinite(transformedX)||!Double.isFinite(transformedY)) {
                throw new IllegalArgumentException("transformed path coordinates must be finite");
            }
            ensureCapacity(pointCount+1);
            coordinates[pointCount*2]=transformedX;
            coordinates[pointCount*2+1]=transformedY;
            pointCount++;
            lastX=x;
            lastY=y;
        }
        private void close(double startX, double startY) {
            if (pointCount>0&&(lastX!=startX||lastY!=startY)) {
                add(startX, startY);
            }
            closed=true;
        }
        private int getPointCount() {
            return pointCount;
        }
        private double[] getCoordinates() {
            double[] result=new double[pointCount*2];
            System.arraycopy(coordinates, 0, result, 0, result.length);
            return result;
        }
        private void ensureCapacity(int required) {
            if (required<=coordinates.length/2) {
                return;
            }
            int capacity=coordinates.length;
            while (capacity<required*2) {
                capacity*=2;
            }
            coordinates=java.util.Arrays.copyOf(coordinates, capacity);
        }
    }
    private static final class CubicBuilder {
        private final SvgTransform transform;
        private double[] coordinates=new double[32];
        private int segmentCount;
        private boolean hasCurve;
        private CubicBuilder(SvgTransform transform) {
            this.transform=transform;
        }
        private void startFromFallback(PathBuilder fallback) {
            if (hasCurve) {
                return;
            }
            for (int index=1;index<fallback.pointCount;index++) {
                int previous=(index-1)*2;
                int current=index*2;
                addTransformedLine(fallback.coordinates[previous], fallback.coordinates[previous+1], fallback.coordinates[current], fallback.coordinates[current+1]);
            }
            hasCurve=true;
        }
        private void addCubic(double startX, double startY, double control1X, double control1Y, double control2X, double control2Y, double endX, double endY) {
            add(startX, startY, control1X, control1Y, control2X, control2Y, endX, endY);
            hasCurve=true;
        }
        private void addLine(double startX, double startY, double endX, double endY) {
            double control1X=startX+(endX-startX)/3.0;
            double control1Y=startY+(endY-startY)/3.0;
            double control2X=startX+2.0*(endX-startX)/3.0;
            double control2Y=startY+2.0*(endY-startY)/3.0;
            add(startX, startY, control1X, control1Y, control2X, control2Y, endX, endY);
        }
        private void add(double startX, double startY, double control1X, double control1Y, double control2X, double control2Y, double endX, double endY) {
            addTransformed(transform.transformX(startX, startY), transform.transformY(startX, startY), transform.transformX(control1X, control1Y), transform.transformY(control1X, control1Y), transform.transformX(control2X, control2Y), transform.transformY(control2X, control2Y), transform.transformX(endX, endY), transform.transformY(endX, endY));
        }
        private void addTransformedLine(double startX, double startY, double endX, double endY) {
            double control1X=startX+(endX-startX)/3.0;
            double control1Y=startY+(endY-startY)/3.0;
            double control2X=startX+2.0*(endX-startX)/3.0;
            double control2Y=startY+2.0*(endY-startY)/3.0;
            addTransformed(startX, startY, control1X, control1Y, control2X, control2Y, endX, endY);
        }
        private void addTransformed(double startX, double startY, double control1X, double control1Y, double control2X, double control2Y, double endX, double endY) {
            if (segmentCount>=MAX_POINTS) {
                throw new IllegalArgumentException("cubic segment count exceeds the supported limit");
            }
            if (!Double.isFinite(startX)||!Double.isFinite(startY)||!Double.isFinite(control1X)||!Double.isFinite(control1Y)||!Double.isFinite(control2X)||!Double.isFinite(control2Y)||!Double.isFinite(endX)||!Double.isFinite(endY)) {
                throw new IllegalArgumentException("cubic coordinates must be finite");
            }
            ensureCapacity(segmentCount+1);
            int offset=segmentCount*8;
            coordinates[offset]=startX;
            coordinates[offset+1]=startY;
            coordinates[offset+2]=control1X;
            coordinates[offset+3]=control1Y;
            coordinates[offset+4]=control2X;
            coordinates[offset+5]=control2Y;
            coordinates[offset+6]=endX;
            coordinates[offset+7]=endY;
            segmentCount++;
        }
        private boolean hasCurve() {
            return hasCurve;
        }
        private double[] getCoordinates() {
            double[] result=new double[segmentCount*8];
            System.arraycopy(coordinates, 0, result, 0, result.length);
            return result;
        }
        private void ensureCapacity(int required) {
            int requiredCoordinates=required*8;
            if (requiredCoordinates<=coordinates.length) {
                return;
            }
            int capacity=coordinates.length;
            while (capacity<requiredCoordinates) {
                capacity*=2;
            }
            coordinates=java.util.Arrays.copyOf(coordinates, capacity);
        }
    }
}
