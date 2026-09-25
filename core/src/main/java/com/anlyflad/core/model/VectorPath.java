package com.anlyflad.core.model;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
public final class VectorPath {
    private static final int MAX_COMPOUND_AREA_SEGMENTS=100000;
    private static final long MAX_COMPOUND_AREA_WORK=100000000L;
    public enum FillRule {
        NONZERO("nonzero"),
        EVEN_ODD("evenodd");
        public static final FillRule EVENODD=EVEN_ODD;
        public static final FillRule NON_ZERO=NONZERO;
        private final String svgValue;
        FillRule(String svgValue) {
            this.svgValue=svgValue;
        }
        public String getSvgValue() {
            return svgValue;
        }
        public String getValue() {
            return svgValue;
        }
        public boolean isEvenOdd() {
            return this==EVEN_ODD;
        }
        public static FillRule parse(String value) {
            if (value==null||value.trim().isEmpty()) {
                throw new IllegalArgumentException("fill rule must be evenodd or nonzero");
            }
            String normalized=value.trim().toLowerCase(Locale.ROOT);
            if (normalized.equals("evenodd")||normalized.equals("even-odd")) {
                return EVEN_ODD;
            }
            if (normalized.equals("nonzero")||normalized.equals("non-zero")) {
                return NONZERO;
            }
            throw new IllegalArgumentException("fill rule must be evenodd or nonzero");
        }
        public static FillRule fromSvgValue(String value) {
            return parse(value);
        }
    }
    private final PathId id;
    private final double[][] rings;
    private final boolean closed;
    private final boolean compound;
    private final FillRule fillRule;
    private final Color fill;
    private final double opacity;
    private final Rect bounds;
    private final double area;
    private final int nodeCount;
    private final double[][] cubicRings;
    public VectorPath(PathId id, double[] coordinates, boolean closed, Color fill, double opacity) {
        this(id, singleRing(coordinates), closed, fill, opacity, FillRule.NONZERO, false, null);
    }
    public VectorPath(PathId id, double[] coordinates, boolean closed, Color fill, double opacity, FillRule fillRule) {
        this(id, singleRing(coordinates), closed, fill, opacity, fillRule, false, null);
    }
    public VectorPath(PathId id, double[] coordinates, double[] cubicRing, boolean closed, Color fill, double opacity) {
        this(id, singleRing(coordinates), closed, fill, opacity, FillRule.NONZERO, false, cubicRing==null?null:new double[][]{cubicRing});
    }
    public VectorPath(PathId id, double[] coordinates, double[] cubicRing, boolean closed, Color fill, double opacity, FillRule fillRule) {
        this(id, singleRing(coordinates), closed, fill, opacity, fillRule, false, cubicRing==null?null:new double[][]{cubicRing});
    }
    public VectorPath(PathId id, List<double[]> rings, Color fill, double opacity, FillRule fillRule) {
        this(id, rings, true, fill, opacity, fillRule, true, null);
    }
    public VectorPath(PathId id, List<double[]> rings, Color fill, double opacity, FillRule fillRule, double area) {
        this(id, rings, true, fill, opacity, fillRule, true, null, area);
    }
    public VectorPath(PathId id, List<double[]> rings, FillRule fillRule, Color fill, double opacity) {
        this(id, rings, fill, opacity, fillRule);
    }
    public VectorPath(PathId id, double[][] rings, Color fill, double opacity, FillRule fillRule) {
        this(id, asList(rings), fill, opacity, fillRule);
    }
    public VectorPath(PathId id, double[][] rings, FillRule fillRule, Color fill, double opacity) {
        this(id, rings, fill, opacity, fillRule);
    }
    public VectorPath(PathId id, List<double[]> rings, double[][] cubicRings, Color fill, double opacity, FillRule fillRule) {
        this(id, rings, true, fill, opacity, fillRule, true, cubicRings);
    }
    public VectorPath(PathId id, List<double[]> rings, double[][] cubicRings, Color fill, double opacity, FillRule fillRule, double area) {
        this(id, rings, true, fill, opacity, fillRule, true, cubicRings, area);
    }
    public VectorPath(PathId id, List<double[]> rings, double[][] cubicRings, FillRule fillRule, Color fill, double opacity) {
        this(id, rings, cubicRings, fill, opacity, fillRule);
    }
    public VectorPath(PathId id, List<double[]> rings, Color fill, double opacity, FillRule fillRule, double[][] cubicRings) {
        this(id, rings, cubicRings, fill, opacity, fillRule);
    }
    public VectorPath(PathId id, double[][] rings, double[][] cubicRings, Color fill, double opacity, FillRule fillRule) {
        this(id, asList(rings), cubicRings, fill, opacity, fillRule);
    }
    public VectorPath(PathId id, double[][] rings, double[][] cubicRings, FillRule fillRule, Color fill, double opacity) {
        this(id, rings, cubicRings, fill, opacity, fillRule);
    }
    public VectorPath(PathId id, double[][] rings, Color fill, double opacity, FillRule fillRule, double[][] cubicRings) {
        this(id, rings, cubicRings, fill, opacity, fillRule);
    }
    private VectorPath(PathId id, List<double[]> sourceRings, boolean closed, Color fill, double opacity, FillRule fillRule, boolean compound, double[][] sourceCubicRings) {
        this(id, sourceRings, closed, fill, opacity, fillRule, compound, sourceCubicRings, Double.NaN);
    }
    private VectorPath(PathId id, List<double[]> sourceRings, boolean closed, Color fill, double opacity, FillRule fillRule, boolean compound, double[][] sourceCubicRings, double knownArea) {
        if (id==null) {
            throw new IllegalArgumentException("id must not be null");
        }
        if (sourceRings==null) {
            throw new IllegalArgumentException(compound?"rings must not be null":"coordinates must not be null");
        }
        if (sourceRings.isEmpty()) {
            throw new IllegalArgumentException(compound?"rings must not be empty":"coordinates must contain at least one x and y pair");
        }
        if (fill==null) {
            throw new IllegalArgumentException("fill must not be null");
        }
        if (fillRule==null) {
            throw new IllegalArgumentException("fillRule must not be null");
        }
        if (!Double.isFinite(opacity)||opacity<0.0||opacity>1.0) {
            throw new IllegalArgumentException("opacity must be between 0 and 1");
        }
        if (!Double.isNaN(knownArea)&&(!Double.isFinite(knownArea)||knownArea<0.0)) {
            throw new IllegalArgumentException("known area must be finite and nonnegative");
        }
        double[][] copiedRings=new double[sourceRings.size()][];
        long totalNodes=0L;
        for (int ringIndex=0;ringIndex<sourceRings.size();ringIndex++) {
            double[] ring=sourceRings.get(ringIndex);
            if (ring==null) {
                throw new IllegalArgumentException("rings must not contain null");
            }
            if (ring.length==0||ring.length%2!=0) {
                throw new IllegalArgumentException(compound?"each ring must contain coordinate pairs":"coordinates must contain at least one x and y pair");
            }
            if (compound&&ring.length<6) {
                throw new IllegalArgumentException("each compound ring must contain at least three coordinate pairs");
            }
            for (int coordinate=0;coordinate<ring.length;coordinate++) {
                if (!Double.isFinite(ring[coordinate])) {
                    throw new IllegalArgumentException("coordinates must be finite");
                }
            }
            copiedRings[ringIndex]=Arrays.copyOf(ring, ring.length);
            totalNodes+=ring.length/2;
        }
        if (totalNodes>Integer.MAX_VALUE) {
            throw new IllegalArgumentException("path has too many nodes");
        }
        this.id=id;
        this.rings=copiedRings;
        this.closed=closed;
        this.compound=compound;
        this.fillRule=fillRule;
        this.fill=fill;
        this.opacity=opacity;
        this.area=Double.isNaN(knownArea)?calculateArea(this.rings, closed, fillRule, compound):knownArea;
        this.nodeCount=(int)totalNodes;
        this.cubicRings=copyCubicRings(sourceCubicRings, this.rings, closed);
        this.bounds=calculateBounds(this.rings, this.cubicRings);
    }
    public PathId getId() {
        return id;
    }
    public double[] getCoordinates() {
        return Arrays.copyOf(rings[0], rings[0].length);
    }
    public List<double[]> getRings() {
        List<double[]> copies=new ArrayList<double[]>(rings.length);
        for (int index=0;index<rings.length;index++) {
            copies.add(Arrays.copyOf(rings[index], rings[index].length));
        }
        return Collections.unmodifiableList(copies);
    }
    public double[][] getRingCoordinates() {
        return copyRings(rings);
    }
    public List<double[]> getFallbackRings() {
        return getRings();
    }
    public double[][] getFallbackRingCoordinates() {
        return getRingCoordinates();
    }
    public double[] getRing(int index) {
        return Arrays.copyOf(rings[index], rings[index].length);
    }
    public int getRingCount() {
        return rings.length;
    }
    public boolean hasCubicData() {
        if (cubicRings==null) {
            return false;
        }
        for (int index=0;index<cubicRings.length;index++) {
            if (cubicRings[index]!=null) {
                return true;
            }
        }
        return false;
    }
    public boolean hasCubicSubpaths() {
        return hasCubicData();
    }
    public boolean isCubic() {
        return hasCubicData();
    }
    public List<double[]> getCubicRings() {
        if (cubicRings==null) {
            return Collections.emptyList();
        }
        List<double[]> copies=new ArrayList<double[]>(cubicRings.length);
        for (int index=0;index<cubicRings.length;index++) {
            copies.add(cubicRings[index]==null?null:Arrays.copyOf(cubicRings[index], cubicRings[index].length));
        }
        return Collections.unmodifiableList(copies);
    }
    public List<double[]> getCubicSubpaths() {
        return getCubicRings();
    }
    public List<double[]> getCubicSegments() {
        if (cubicRings==null) {
            return Collections.emptyList();
        }
        List<double[]> segments=new ArrayList<double[]>(getCubicSegmentCount());
        for (int ringIndex=0;ringIndex<cubicRings.length;ringIndex++) {
            if (cubicRings[ringIndex]==null) {
                continue;
            }
            for (int offset=0;offset<cubicRings[ringIndex].length;offset+=8) {
                segments.add(Arrays.copyOfRange(cubicRings[ringIndex],offset,offset+8));
            }
        }
        return Collections.unmodifiableList(segments);
    }
    public int getCubicSubpathCount() {
        return getCubicRingCount();
    }
    public List<double[]> getCubicCommandSegments() {
        List<double[]> commands=new ArrayList<double[]>(getCubicSegmentCount());
        for (double[] segment:getCubicSegments()) {
            commands.add(Arrays.copyOfRange(segment,2,8));
        }
        return Collections.unmodifiableList(commands);
    }
    public List<List<double[]>> getCubicSegmentLists() {
        if (cubicRings==null) {
            return Collections.emptyList();
        }
        List<List<double[]>> result=new ArrayList<List<double[]>>(cubicRings.length);
        for (int index=0;index<cubicRings.length;index++) {
            if (cubicRings[index]==null) {
                result.add(null);
            } else {
                List<double[]> segments=new ArrayList<double[]>(cubicRings[index].length/8);
                for (int offset=0;offset<cubicRings[index].length;offset+=8) {
                    segments.add(Arrays.copyOfRange(cubicRings[index],offset,offset+8));
                }
                result.add(Collections.unmodifiableList(segments));
            }
        }
        return Collections.unmodifiableList(result);
    }
    public double[][] getCubicRingCoordinates() {
        return cubicRings==null?new double[0][]:copyCubicRings(cubicRings);
    }
    public double[][] getCubicData() {
        return getCubicRingCoordinates();
    }
    public double[][] getCubicRingsArray() {
        return getCubicRingCoordinates();
    }
    public double[] getCubicRing(int index) {
        if (cubicRings==null||index<0||index>=cubicRings.length||cubicRings[index]==null) {
            throw new IllegalArgumentException("cubic ring index is invalid");
        }
        return Arrays.copyOf(cubicRings[index], cubicRings[index].length);
    }
    public int getCubicRingCount() {
        return cubicRings==null?0:numberOfCubicRings(cubicRings);
    }
    public int getCubicSegmentCount() {
        int count=0;
        if (cubicRings!=null) {
            for (int index=0;index<cubicRings.length;index++) {
                if (cubicRings[index]!=null) {
                    count+=cubicRings[index].length/8;
                }
            }
        }
        return count;
    }
    public double[] getCubicSegment(int ringIndex, int segmentIndex) {
        if (cubicRings==null||ringIndex<0||ringIndex>=cubicRings.length||cubicRings[ringIndex]==null) {
            throw new IllegalArgumentException("cubic ring index is invalid");
        }
        int segmentCount=cubicRings[ringIndex].length/8;
        if (segmentIndex<0||segmentIndex>=segmentCount) {
            throw new IllegalArgumentException("cubic segment index is invalid");
        }
        int offset=segmentIndex*8;
        return Arrays.copyOfRange(cubicRings[ringIndex], offset, offset+8);
    }
    public boolean isCompound() {
        return compound;
    }
    public FillRule getFillRule() {
        return fillRule;
    }
    public boolean isClosed() {
        return closed;
    }
    public Color getFill() {
        return fill;
    }
    public double getOpacity() {
        return opacity;
    }
    public Rect getBounds() {
        return bounds;
    }
    public double getArea() {
        return area;
    }
    public int getNodeCount() {
        return nodeCount;
    }
    public VectorPath withGeometry(double[] coordinates, boolean closed) {
        if (!compound) {
            return new VectorPath(id, coordinates, closed, fill, opacity, fillRule);
        }
        return new VectorPath(id, singleRing(coordinates), closed, fill, opacity, fillRule, true, null);
    }
    public VectorPath withRings(List<double[]> rings) {
        return new VectorPath(id, rings, fill, opacity, fillRule);
    }
    public VectorPath withRings(List<double[]> rings, double[][] cubicRings) {
        return new VectorPath(id, rings, closed, fill, opacity, fillRule, compound, cubicRings);
    }
    public VectorPath withRingsAndArea(List<double[]> rings, double[][] cubicRings, double area) {
        return new VectorPath(id, rings, closed, fill, opacity, fillRule, compound, cubicRings, area);
    }
    public VectorPath withCubicRings(double[][] cubicRings) {
        return new VectorPath(id, asList(rings), closed, fill, opacity, fillRule, compound, cubicRings);
    }
    public VectorPath withCubicSubpaths(double[][] cubicRings) {
        return withCubicRings(cubicRings);
    }
    public VectorPath withCubicSegments(double[][][] cubicSegments) {
        List<double[]> fallback=getRings();
        return new VectorPath(id, fallback, closed, fill, opacity, fillRule, compound, flattenCubicSegments(cubicSegments, fallback));
    }
    public VectorPath withoutCubicData() {
        return cubicRings==null?this:withCubicRings(null);
    }
    public VectorPath withStyle(Color fill, double opacity) {
        return new VectorPath(id, asList(rings), closed, fill, opacity, fillRule, compound, cubicRings);
    }
    public VectorPath withFillRule(FillRule fillRule) {
        return new VectorPath(id, asList(rings), closed, fill, opacity, fillRule, compound, cubicRings);
    }
    @Override
    public boolean equals(Object other) {
        if (this==other) {
            return true;
        }
        if (other==null||getClass()!=other.getClass()) {
            return false;
        }
        VectorPath path=(VectorPath)other;
        return closed==path.closed&&compound==path.compound&&fillRule==path.fillRule&&id.equals(path.id)&&ringsEqual(rings, path.rings)&&cubicRingsEqual(cubicRings, path.cubicRings)&&fill.equals(path.fill)&&Double.doubleToLongBits(opacity)==Double.doubleToLongBits(path.opacity);
    }
    @Override
    public int hashCode() {
        int result=17;
        result=31*result+id.hashCode();
        result=31*result+Arrays.deepHashCode(rings);
        result=31*result+(closed?1:0);
        result=31*result+(compound?1:0);
        result=31*result+fill.hashCode();
        result=31*result+(fillRule.isEvenOdd()?1:0);
        result=31*result+longBits(opacity);
        result=31*result+bounds.hashCode();
        result=31*result+longBits(area);
        result=31*result+nodeCount;
        if (cubicRings!=null) {
            result=31*result+cubicRingsHash(cubicRings);
        }
        return result;
    }
    @Override
    public String toString() {
        if (!compound) {
            String value="VectorPath{id="+id+", coordinates="+Arrays.toString(rings[0])+", closed="+closed+", fill="+fill+", opacity="+opacity+", bounds="+bounds+", area="+area+", nodeCount="+nodeCount+"}";
            return hasCubicData()?value.substring(0, value.length()-1)+", cubicRings="+Arrays.deepToString(cubicRings)+"}":value;
        }
        String value="VectorPath{id="+id+", rings="+Arrays.deepToString(rings)+", closed="+closed+", fill="+fill+", fillRule="+fillRule.getSvgValue()+", opacity="+opacity+", bounds="+bounds+", area="+area+", nodeCount="+nodeCount+"}";
        return hasCubicData()?value.substring(0, value.length()-1)+", cubicRings="+Arrays.deepToString(cubicRings)+"}":value;
    }
    private static List<double[]> singleRing(double[] coordinates) {
        if (coordinates==null) {
            throw new IllegalArgumentException("coordinates must not be null");
        }
        return Collections.singletonList(coordinates);
    }
    private static List<double[]> asList(double[][] source) {
        if (source==null) {
            throw new IllegalArgumentException("rings must not be null");
        }
        List<double[]> rings=new ArrayList<double[]>(source.length);
        for (int index=0;index<source.length;index++) {
            rings.add(source[index]);
        }
        return rings;
    }
    private static double[][] copyRings(double[][] source) {
        double[][] copy=new double[source.length][];
        for (int index=0;index<source.length;index++) {
            copy[index]=Arrays.copyOf(source[index], source[index].length);
        }
        return copy;
    }
    private static boolean ringsEqual(double[][] first, double[][] second) {
        if (first.length!=second.length) {
            return false;
        }
        for (int index=0;index<first.length;index++) {
            if (!Arrays.equals(first[index], second[index])) {
                return false;
            }
        }
        return true;
    }
    private static double[][] copyCubicRings(double[][] source) {
        if (source==null) {
            return null;
        }
        double[][] copy=new double[source.length][];
        boolean present=false;
        for (int index=0;index<source.length;index++) {
            copy[index]=source[index]==null?null:Arrays.copyOf(source[index], source[index].length);
            present|=copy[index]!=null;
        }
        return present?copy:null;
    }
    private static double[][] copyCubicRings(double[][] source, double[][] fallback, boolean closed) {
        if (source==null||source.length==0) {
            return null;
        }
        double[][] normalizedSource=source;
        if (source.length!=fallback.length&&fallback.length==1&&isCubicSegmentRows(source)) {
            double[] flattened=new double[source.length*8];
            for (int index=0;index<source.length;index++) {
                System.arraycopy(source[index],0,flattened,index*8,8);
            }
            normalizedSource=new double[][]{flattened};
        }
        if (normalizedSource.length!=fallback.length) {
            throw new IllegalArgumentException("cubic ring count must equal line ring count");
        }
        double[][] copy=new double[normalizedSource.length][];
        boolean present=false;
        for (int index=0;index<normalizedSource.length;index++) {
            copy[index]=normalizeCubicRing(normalizedSource[index], fallback[index], closed);
            present|=copy[index]!=null;
        }
        return present?copy:null;
    }
    private static boolean isCubicSegmentRows(double[][] source) {
        if (source.length==0) {
            return false;
        }
        for (int index=0;index<source.length;index++) {
            if (source[index]==null||source[index].length!=8) {
                return false;
            }
        }
        return true;
    }
    private static double[] normalizeCubicRing(double[] source, double[] fallback, boolean closed) {
        if (source==null) {
            return null;
        }
        if (fallback==null||fallback.length<2) {
            throw new IllegalArgumentException("line ring fallback must contain coordinate pairs");
        }
        int segmentCount;
        int offset;
        int stride;
        if (source.length>=8&&source.length%8==0) {
            segmentCount=source.length/8;
            offset=0;
            stride=8;
        } else if (source.length>=8&&(source.length-2)%6==0) {
            segmentCount=(source.length-2)/6;
            offset=2;
            stride=6;
        } else if (source.length>=6&&source.length%6==0) {
            segmentCount=source.length/6;
            offset=0;
            stride=6;
        } else {
            throw new IllegalArgumentException("cubic ring must contain complete cubic segments");
        }
        if (segmentCount<1) {
            throw new IllegalArgumentException("cubic ring must contain at least one segment");
        }
        double[] result=new double[segmentCount*8];
        double currentX=fallback[0];
        double currentY=fallback[1];
        for (int index=0;index<segmentCount;index++) {
            int sourceOffset=offset+index*stride;
            double startX;
            double startY;
            if (stride==8) {
                startX=source[sourceOffset];
                startY=source[sourceOffset+1];
            } else {
                startX=currentX;
                startY=currentY;
                if (offset==0&&index==0) {
                    startX=fallback[0];
                    startY=fallback[1];
                }
            }
            if (!samePoint(startX,startY,currentX,currentY)) {
                throw new IllegalArgumentException("cubic segments must be continuous");
            }
            int target=index*8;
            result[target]=currentX;
            result[target+1]=currentY;
            int controlOffset=stride==8?sourceOffset+2:sourceOffset;
            result[target+2]=source[controlOffset];
            result[target+3]=source[controlOffset+1];
            result[target+4]=source[controlOffset+2];
            result[target+5]=source[controlOffset+3];
            result[target+6]=source[controlOffset+4];
            result[target+7]=source[controlOffset+5];
            for (int coordinate=target;coordinate<target+8;coordinate++) {
                if (!Double.isFinite(result[coordinate])) {
                    throw new IllegalArgumentException("cubic coordinates must be finite");
                }
            }
            currentX=result[target+6];
            currentY=result[target+7];
        }
        double expectedX=closed?fallback[0]:fallback[fallback.length-2];
        double expectedY=closed?fallback[1]:fallback[fallback.length-1];
        if (!samePoint(currentX,currentY,expectedX,expectedY)) {
            throw new IllegalArgumentException("cubic ring must preserve its endpoints");
        }
        return result;
    }
    private static boolean samePoint(double firstX, double firstY, double secondX, double secondY) {
        return firstX==secondX&&firstY==secondY;
    }
    private static int numberOfCubicRings(double[][] source) {
        int count=0;
        for (int index=0;index<source.length;index++) {
            if (source[index]!=null) {
                count++;
            }
        }
        return count;
    }
    private static boolean cubicRingsEqual(double[][] first, double[][] second) {
        boolean firstPresent=first!=null;
        boolean secondPresent=second!=null;
        if (!firstPresent||!secondPresent) {
            return firstPresent==secondPresent;
        }
        return ringsEqual(first,second);
    }
    private static int cubicRingsHash(double[][] source) {
        return source==null?0:Arrays.deepHashCode(source);
    }
    private static double[][] flattenCubicSegments(double[][][] source, List<double[]> fallback) {
        if (source==null) {
            return null;
        }
        if (fallback==null||source.length!=fallback.size()) {
            throw new IllegalArgumentException("cubic ring count must equal line ring count");
        }
        double[][] result=new double[source.length][];
        for (int ringIndex=0;ringIndex<source.length;ringIndex++) {
            if (source[ringIndex]==null) {
                result[ringIndex]=null;
                continue;
            }
            double[] lineRing=fallback.get(ringIndex);
            if (lineRing==null||lineRing.length<2) {
                throw new IllegalArgumentException("line ring fallback must contain coordinate pairs");
            }
            result[ringIndex]=new double[source[ringIndex].length*8];
            double currentX=lineRing[0];
            double currentY=lineRing[1];
            for (int segmentIndex=0;segmentIndex<source[ringIndex].length;segmentIndex++) {
                double[] segment=source[ringIndex][segmentIndex];
                if (segment==null||(segment.length!=6&&segment.length!=8)) {
                    throw new IllegalArgumentException("cubic segments must contain six or eight coordinates");
                }
                int target=segmentIndex*8;
                if (segment.length==8) {
                    if (!samePoint(segment[0],segment[1],currentX,currentY)) {
                        throw new IllegalArgumentException("cubic segments must be continuous");
                    }
                    System.arraycopy(segment,0,result[ringIndex],target,8);
                    currentX=segment[6];
                    currentY=segment[7];
                } else {
                    result[ringIndex][target]=currentX;
                    result[ringIndex][target+1]=currentY;
                    System.arraycopy(segment,0,result[ringIndex],target+2,6);
                    currentX=segment[4];
                    currentY=segment[5];
                }
            }
        }
        return result;
    }
    private static Rect calculateBounds(double[][] rings) {
        return calculateBounds(rings, null);
    }
    private static Rect calculateBounds(double[][] rings, double[][] cubicRings) {
        double minX=rings[0][0];
        double minY=rings[0][1];
        double maxX=minX;
        double maxY=minY;
        for (int ringIndex=0;ringIndex<rings.length;ringIndex++) {
            double[] coordinates=rings[ringIndex];
            for (int index=0;index<coordinates.length;index+=2) {
                minX=Math.min(minX, coordinates[index]);
                minY=Math.min(minY, coordinates[index+1]);
                maxX=Math.max(maxX, coordinates[index]);
                maxY=Math.max(maxY, coordinates[index+1]);
            }
        }
        if (cubicRings!=null) {
            for (int ringIndex=0;ringIndex<cubicRings.length;ringIndex++) {
                double[] coordinates=cubicRings[ringIndex];
                if (coordinates==null) {
                    continue;
                }
                for (int index=0;index<coordinates.length;index+=2) {
                    minX=Math.min(minX, coordinates[index]);
                    minY=Math.min(minY, coordinates[index+1]);
                    maxX=Math.max(maxX, coordinates[index]);
                    maxY=Math.max(maxY, coordinates[index+1]);
                }
            }
        }
        return new Rect(minX, minY, maxX, maxY);
    }
    private static double calculateArea(double[][] rings, boolean closed, FillRule fillRule, boolean compound) {
        if (!closed) {
            return 0.0;
        }
        if (!compound||rings.length==1) {
            return calculateRingArea(rings[0]);
        }
        return calculateCompoundArea(rings, fillRule);
    }
    private static double calculateRingArea(double[] coordinates) {
        double sum=0.0;
        for (int index=0;index<coordinates.length;index+=2) {
            int nextIndex=(index+2)%coordinates.length;
            sum+=coordinates[index]*coordinates[nextIndex+1]-coordinates[nextIndex]*coordinates[index+1];
        }
        return Math.abs(sum)*0.5;
    }
    private static double calculateCompoundArea(double[][] rings, FillRule fillRule) {
        List<Segment> segments=new ArrayList<Segment>();
        List<Double> xValues=new ArrayList<Double>();
        for (int ringIndex=0;ringIndex<rings.length;ringIndex++) {
            double[] coordinates=rings[ringIndex];
            int pointCount=coordinates.length/2;
            for (int index=0;index<pointCount;index++) {
                int nextIndex=(index+1)%pointCount;
                double x1=coordinates[index*2];
                double y1=coordinates[index*2+1];
                double x2=coordinates[nextIndex*2];
                double y2=coordinates[nextIndex*2+1];
                if (x1==x2&&y1==y2) {
                    continue;
                }
                segments.add(new Segment(x1, y1, x2, y2));
                xValues.add(x1);
                xValues.add(x2);
            }
        }
        if (segments.size()>MAX_COMPOUND_AREA_SEGMENTS) {
            throw new IllegalArgumentException("compound path has too many segments for area calculation");
        }
        long intersectionWork=(long)segments.size()*(long)(segments.size()-1)/2L;
        if (intersectionWork>MAX_COMPOUND_AREA_WORK) {
            throw new IllegalArgumentException("compound path area calculation exceeds its work limit");
        }
        for (int first=0;first<segments.size();first++) {
            for (int second=first+1;second<segments.size();second++) {
                addIntersectionX(xValues, segments.get(first), segments.get(second));
            }
        }
        Collections.sort(xValues);
        double area=0.0;
        for (int index=0;index+1<xValues.size();index++) {
            double left=xValues.get(index);
            double right=xValues.get(index+1);
            if (right<=left) {
                continue;
            }
            double x=left/2.0+right/2.0;
            List<Crossing> crossings=new ArrayList<Crossing>();
            for (int segmentIndex=0;segmentIndex<segments.size();segmentIndex++) {
                Segment segment=segments.get(segmentIndex);
                if (segment.x1==segment.x2) {
                    continue;
                }
                double minX=Math.min(segment.x1, segment.x2);
                double maxX=Math.max(segment.x1, segment.x2);
                if (x<=minX||x>=maxX) {
                    continue;
                }
                double ratio=(x-segment.x1)/(segment.x2-segment.x1);
                double y=segment.y1+(segment.y2-segment.y1)*ratio;
                int windingDelta;
                if (segment.y1==segment.y2) {
                    windingDelta=segment.x2>segment.x1?1:-1;
                } else {
                    windingDelta=segment.y2>segment.y1?1:-1;
                }
                crossings.add(new Crossing(y, windingDelta));
            }
            Collections.sort(crossings);
            boolean odd=false;
            int winding=0;
            double filledHeight=0.0;
            for (int crossingIndex=0;crossingIndex+1<crossings.size();crossingIndex++) {
                Crossing crossing=crossings.get(crossingIndex);
                if (fillRule.isEvenOdd()) {
                    odd=!odd;
                } else {
                    winding+=crossing.windingDelta;
                }
                if (odd||winding!=0) {
                    filledHeight+=crossings.get(crossingIndex+1).y-crossing.y;
                }
            }
            area+=(right-left)*filledHeight;
        }
        return area;
    }
    private static void addIntersectionX(List<Double> xValues, Segment first, Segment second) {
        double rx=first.x2-first.x1;
        double ry=first.y2-first.y1;
        double sx=second.x2-second.x1;
        double sy=second.y2-second.y1;
        double denominator=rx*sy-ry*sx;
        if (denominator==0.0) {
            return;
        }
        double qpx=second.x1-first.x1;
        double qpy=second.y1-first.y1;
        double t=(qpx*sy-qpy*sx)/denominator;
        double u=(qpx*ry-qpy*rx)/denominator;
        if (t>=0.0&&t<=1.0&&u>=0.0&&u<=1.0) {
            double x=first.x1+t*rx;
            if (Double.isFinite(x)) {
                xValues.add(x);
            }
        }
    }
    private static int longBits(double value) {
        long bits=Double.doubleToLongBits(value);
        return (int)(bits^(bits>>>32));
    }
    private static final class Segment {
        private final double x1;
        private final double y1;
        private final double x2;
        private final double y2;
        private Segment(double x1, double y1, double x2, double y2) {
            this.x1=x1;
            this.y1=y1;
            this.x2=x2;
            this.y2=y2;
        }
    }
    private static final class Crossing implements Comparable<Crossing> {
        private final double y;
        private final int windingDelta;
        private Crossing(double y, int windingDelta) {
            this.y=y;
            this.windingDelta=windingDelta;
        }
        public int compareTo(Crossing other) {
            return Double.compare(y, other.y);
        }
    }
}
