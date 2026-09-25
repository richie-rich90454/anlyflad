package com.vectorium.core.geometry;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public final class CubicBezierFitter {
    public static final double DEFAULT_TOLERANCE=0.5;
    public static final int DEFAULT_MAX_DEPTH=20;
    public static final int DEFAULT_MAX_SEGMENTS=100000;
    public static final int DEFAULT_MAX_POINTS=1000000;
    private static final int PARAMETER_ITERATIONS=4;
    private static final int ERROR_SAMPLES=256;
    private static final int FINAL_SAMPLES=1024;
    private static final double MIN_AREA=1.0e-12;
    private static final long MAX_WORK=100000000L;
    private final double tolerance;
    private final int maxDepth;
    private final int maxSegments;
    private final int maxPoints;
    private double[] x;
    private double[] y;
    private int pointCount;
    private double minimumX;
    private double minimumY;
    private double maximumX;
    private double maximumY;
    private double coordinateScale;
    private double areaScale;
    private List<double[]> result;
    private List<int[]> ranges;
    private int segmentCount;
    private long work;
    private double inputSignedArea;
    private boolean rectilinear;
    private boolean exactRectilinear;
    private double lastError;

    public CubicBezierFitter() {
        this(DEFAULT_TOLERANCE);
    }

    public CubicBezierFitter(double tolerance) {
        this(tolerance, DEFAULT_MAX_DEPTH, DEFAULT_MAX_SEGMENTS, DEFAULT_MAX_POINTS);
    }

    public CubicBezierFitter(double tolerance, int maxDepth) {
        this(tolerance, maxDepth, DEFAULT_MAX_SEGMENTS, DEFAULT_MAX_POINTS);
    }

    public CubicBezierFitter(double tolerance, int maxDepth, int maxSegments) {
        this(tolerance, maxDepth, maxSegments, DEFAULT_MAX_POINTS);
    }

    public CubicBezierFitter(double tolerance, int maxDepth, int maxSegments, int maxPoints) {
        if (!Double.isFinite(tolerance)||tolerance<0.0) {
            throw new IllegalArgumentException("tolerance must be finite and nonnegative");
        }
        if (maxDepth<1||maxDepth>32) {
            throw new IllegalArgumentException("maxDepth must be between 1 and 32");
        }
        if (maxSegments<1||maxSegments>1000000) {
            throw new IllegalArgumentException("maxSegments must be between 1 and 1000000");
        }
        if (maxPoints<3||maxPoints>4000000) {
            throw new IllegalArgumentException("maxPoints must be between 3 and 4000000");
        }
        this.tolerance=tolerance;
        this.maxDepth=maxDepth;
        this.maxSegments=maxSegments;
        this.maxPoints=maxPoints;
    }

    public List<double[]> fit(double[] ring) {
        if (ring==null) {
            throw new IllegalArgumentException("ring must not be null");
        }
        if (ring.length==0||ring.length%2!=0) {
            throw new IllegalArgumentException("ring must contain coordinate pairs");
        }
        int sourceCount=ring.length/2;
        int closedCount=sourceCount;
        if (closedCount>1&&same(ring[0],ring[1],ring[(closedCount-1)*2],ring[(closedCount-1)*2+1])) {
            closedCount--;
        }
        if (closedCount>maxPoints) {
            throw new IllegalArgumentException("ring has too many points");
        }
        if (closedCount<3) {
            throw new IllegalArgumentException("ring must contain at least three distinct points");
        }
        x=new double[closedCount+1];
        y=new double[closedCount+1];
        for (int index=0;index<closedCount;index++) {
            x[index]=ring[index*2];
            y[index]=ring[index*2+1];
            if (!Double.isFinite(x[index])||!Double.isFinite(y[index])) {
                throw new IllegalArgumentException("ring coordinates must be finite");
            }
        }
        x[closedCount]=x[0];
        y[closedCount]=y[0];
        pointCount=closedCount+1;
        for (int index=0;index<closedCount;index++) {
            int next=(index+1)%closedCount;
            if (same(x[index],y[index],x[next],y[next])) {
                throw new IllegalArgumentException("ring must not contain duplicate adjacent points");
            }
        }
        initializeBounds();
        work=0L;
        result=new ArrayList<double[]>(Math.min(maxSegments, 256));
        ranges=new ArrayList<int[]>(Math.min(maxSegments, 256));
        segmentCount=0;
        lastError=0.0;
        exactRectilinear=false;
        if (rectilinear&&closedCount>256) {
            return copyExactRectilinear(closedCount);
        }
        validateSimpleRing();
        fitRange(0,closedCount,0);
        if (result.isEmpty()) {
            throw new IllegalArgumentException("ring did not produce a cubic fit");
        }
        validateResult();
        List<double[]> copy=new ArrayList<double[]>(result.size());
        for (int index=0;index<result.size();index++) {
            copy.add(Arrays.copyOf(result.get(index),8));
        }
        return Collections.unmodifiableList(copy);
    }

    public List<double[]> fitClosedRing(double[] ring) {
        return fit(ring);
    }

    public List<double[]> fitRing(double[] ring) {
        return fit(ring);
    }

    public static List<double[]> fit(double[] ring, double tolerance) {
        return new CubicBezierFitter(tolerance).fit(ring);
    }

    public static List<double[]> fit(double[] ring, double tolerance, int maxDepth) {
        return new CubicBezierFitter(tolerance, maxDepth).fit(ring);
    }

    public static List<double[]> fit(double[] ring, double tolerance, int maxDepth, int maxSegments) {
        return new CubicBezierFitter(tolerance, maxDepth, maxSegments).fit(ring);
    }

    public static List<double[]> fitClosedRing(double[] ring, double tolerance) {
        return new CubicBezierFitter(tolerance).fit(ring);
    }

    public static List<double[]> fitClosed(double[] ring, double tolerance) {
        return new CubicBezierFitter(tolerance).fit(ring);
    }

    public static List<double[]> fitRing(double[] ring, double tolerance) {
        return new CubicBezierFitter(tolerance).fit(ring);
    }

    public double getLastError() {
        return lastError;
    }

    public double getMaxError() {
        return lastError;
    }

    public double getTolerance() {
        return tolerance;
    }

    private void initializeBounds() {
        minimumX=x[0];
        minimumY=y[0];
        maximumX=x[0];
        maximumY=y[0];
        for (int index=1;index<pointCount;index++) {
            minimumX=Math.min(minimumX,x[index]);
            minimumY=Math.min(minimumY,y[index]);
            maximumX=Math.max(maximumX,x[index]);
            maximumY=Math.max(maximumY,y[index]);
        }
        double span=Math.max(maximumX-minimumX,maximumY-minimumY);
        if (!Double.isFinite(span)) {
            throw new IllegalArgumentException("ring bounds are not finite");
        }
        areaScale=Math.max(1.0,span);
        coordinateScale=areaScale;
        for (int index=0;index<pointCount;index++) {
            coordinateScale=Math.max(coordinateScale,Math.max(Math.abs(x[index]),Math.abs(y[index]))*1.0e-12);
        }
        rectilinear=true;
        for (int index=0;index<pointCount-1;index++) {
            if (x[index]!=x[index+1]&&y[index]!=y[index+1]) {
                rectilinear=false;
                break;
            }
        }
    }

    private List<double[]> copyExactRectilinear(int count) {
        if (count>maxSegments) {
            throw new IllegalArgumentException("cubic fit exceeds the configured segment limit");
        }
        exactRectilinear=true;
        for (int index=0;index<count;index++) {
            int last=index+1==count?count:index+1;
            append(new Candidate(lineControls(index,last),0.0,last),index,last);
        }
        validateResult();
        List<double[]> copy=new ArrayList<double[]>(result.size());
        for (int index=0;index<result.size();index++) {
            copy.add(Arrays.copyOf(result.get(index),8));
        }
        return Collections.unmodifiableList(copy);
    }
    private void validateSimpleRing() {
        double area=0.0;
        double originX=x[0];
        double originY=y[0];
        for (int index=0;index<pointCount-1;index++) {
            double nextX=x[index+1];
            double nextY=y[index+1];
            double term=(x[index]-originX)*(nextY-originY)-(nextX-originX)*(y[index]-originY);
            if (!Double.isFinite(term)) {
                throw new IllegalArgumentException("ring area is not finite");
            }
            area+=term;
        }
        if (!Double.isFinite(area)||Math.abs(area)*0.5<=MIN_AREA*areaScale*areaScale) {
            throw new IllegalArgumentException("ring is degenerate");
        }
        inputSignedArea=area*0.5;
        int count=pointCount-1;
        if (!rectilinear) {
            useWork((long)count*(count-1L)/2L);
            for (int first=0;first<count;first++) {
                int firstNext=(first+1)%count;
                for (int second=first+1;second<count;second++) {
                    int secondNext=(second+1)%count;
                    if (first==second||firstNext==second||secondNext==first) {
                        continue;
                    }
                    if (segmentsIntersect(x[first],y[first],x[firstNext],y[firstNext],x[second],y[second],x[secondNext],y[secondNext])) {
                        throw new IllegalArgumentException("ring must not self-intersect");
                    }
                }
            }
        }
    }

    private void fitRange(int first, int last, int depth) {
        if (last-first<=1) {
            appendLine(first,last);
            return;
        }
        Candidate candidate=fitCandidate(first,last);
        if (candidate.error<=acceptanceTolerance()) {
            append(candidate,first,last);
            return;
        }
        if (depth>=maxDepth) {
            throw new IllegalArgumentException("cubic fit exceeds the configured depth");
        }
        int split=candidate.split;
        if (split<=first||split>=last||split==first+1||split==last-1) {
            split=(first+last)/2;
        }
        if (segmentCount+2>maxSegments) {
            throw new IllegalArgumentException("cubic fit exceeds the configured segment limit");
        }
        fitRange(first,split,depth+1);
        fitRange(split,last,depth+1);
    }

    private Candidate fitCandidate(int first, int last) {
        int count=last-first+1;
        double[] parameters=new double[count];
        double totalLength=0.0;
        for (int index=1;index<count;index++) {
            double dx=x[first+index]-x[first+index-1];
            double dy=y[first+index]-y[first+index-1];
            double length=Math.sqrt(dx*dx+dy*dy);
            if (!Double.isFinite(length)) {
                throw new IllegalArgumentException("ring length is not finite");
            }
            totalLength+=length;
            parameters[index]=totalLength;
        }
        if (!Double.isFinite(totalLength)||totalLength<=coordinateScale*1.0e-14) {
            Candidate line=lineCandidate(first,last);
            line.split=first+1;
            return line;
        }
        for (int index=0;index<count;index++) {
            parameters[index]/=totalLength;
        }
        Candidate best=lineCandidate(first,last);
        double[] controls=new double[4];
        for (int iteration=0;iteration<PARAMETER_ITERATIONS;iteration++) {
            controls=fitControls(first,last,parameters);
            if (!safeControls(controls)) {
                controls=lineControls(first,last);
            }
            double error=measure(first,last,controls,ERROR_SAMPLES);
            int split=farthestPoint(first,last,controls);
            if (error<best.error) {
                best=new Candidate(copyControls(controls),error,split);
            }
            if (iteration+1<PARAMETER_ITERATIONS) {
                reparameterize(first,last,controls,parameters);
            }
        }
        return best;
    }

    private double[] fitControls(int first, int last, double[] parameters) {
        double x0=x[first];
        double y0=y[first];
        double x3=x[last];
        double y3=y[last];
        double c00=0.0;
        double c01=0.0;
        double c11=0.0;
        double rx0=0.0;
        double ry0=0.0;
        double rx1=0.0;
        double ry1=0.0;
        for (int index=1;index<last-first;index++) {
            double t=parameters[index];
            double inverse=1.0-t;
            double a=3.0*inverse*inverse*t;
            double b=3.0*inverse*t*t;
            double baseX=inverse*inverse*inverse*x0+t*t*t*x3;
            double baseY=inverse*inverse*inverse*y0+t*t*t*y3;
            double targetX=x[first+index]-baseX;
            double targetY=y[first+index]-baseY;
            c00+=a*a;
            c01+=a*b;
            c11+=b*b;
            rx0+=a*targetX;
            ry0+=a*targetY;
            rx1+=b*targetX;
            ry1+=b*targetY;
        }
        double determinant=c00*c11-c01*c01;
        if (!Double.isFinite(determinant)||Math.abs(determinant)<=1.0e-14*Math.max(1.0,c00*c11)) {
            return lineControls(first,last);
        }
        double c1x=(rx0*c11-rx1*c01)/determinant;
        double c2x=(c00*rx1-c01*rx0)/determinant;
        double c1y=(ry0*c11-ry1*c01)/determinant;
        double c2y=(c00*ry1-c01*ry0)/determinant;
        return new double[]{c1x,c1y,c2x,c2y};
    }

    private void reparameterize(int first, int last, double[] controls, double[] parameters) {
        int count=last-first+1;
        for (int index=1;index<count-1;index++) {
            double current=parameters[index];
            double lower=parameters[index-1];
            double upper=parameters[index+1];
            double bestT=current;
            double bestDistance=Double.POSITIVE_INFINITY;
            for (int sample=0;sample<=32;sample++) {
                double t=lower+(upper-lower)*sample/32.0;
                double px=evaluate(x[first],controls[0],controls[2],x[last],t);
                double py=evaluate(y[first],controls[1],controls[3],y[last],t);
                double dx=px-x[first+index];
                double dy=py-y[first+index];
                double distance=dx*dx+dy*dy;
                if (distance<bestDistance) {
                    bestDistance=distance;
                    bestT=t;
                }
            }
            parameters[index]=bestT;
        }
        parameters[0]=0.0;
        parameters[count-1]=1.0;
    }

    private Candidate lineCandidate(int first, int last) {
        double[] controls=lineControls(first,last);
        return new Candidate(controls,measure(first,last,controls,ERROR_SAMPLES),farthestPoint(first,last,controls));
    }

    private double[] lineControls(int first, int last) {
        double dx=x[last]-x[first];
        double dy=y[last]-y[first];
        return new double[]{x[first]+dx/3.0,y[first]+dy/3.0,x[first]+2.0*dx/3.0,y[first]+2.0*dy/3.0};
    }

    private double measure(int first, int last, double[] controls, int samples) {
        useWork((long)(last-first)*samples*2L+samples);
        double maximum=0.0;
        for (int index=first+1;index<last;index++) {
            double distance=pointCurveDistance(x[index],y[index],x[first],y[first],controls[0],controls[1],controls[2],controls[3],x[last],y[last],samples);
            if (distance>maximum) {
                maximum=distance;
            }
        }
        for (int sample=1;sample<samples;sample++) {
            double t=sample/(double)samples;
            double px=evaluate(x[first],controls[0],controls[2],x[last],t);
            double py=evaluate(y[first],controls[1],controls[3],y[last],t);
            double distance=pointPolylineDistance(px,py,first,last);
            if (distance>maximum) {
                maximum=distance;
            }
        }
        return maximum;
    }

    private double pointCurveDistance(double px, double py, double x0, double y0, double c1x, double c1y, double c2x, double c2y, double x3, double y3, int samples) {
        double minimum=Double.POSITIVE_INFINITY;
        for (int sample=0;sample<=samples;sample++) {
            double t=sample/(double)samples;
            double dx=evaluate(x0,c1x,c2x,x3,t)-px;
            double dy=evaluate(y0,c1y,c2y,y3,t)-py;
            double distance=dx*dx+dy*dy;
            if (distance<minimum) {
                minimum=distance;
            }
        }
        return Math.sqrt(minimum);
    }

    private double pointPolylineDistance(double px, double py, int first, int last) {
        double minimum=Double.POSITIVE_INFINITY;
        for (int index=first;index<last;index++) {
            double distance=pointSegmentDistance(px,py,x[index],y[index],x[index+1],y[index+1]);
            if (distance<minimum) {
                minimum=distance;
            }
        }
        return minimum;
    }

    private int farthestPoint(int first, int last, double[] controls) {
        useWork((long)(last-first)*32L);
        int farthest=first+1;
        double maximum=-1.0;
        for (int index=first+1;index<last;index++) {
            double distance=pointCurveDistance(x[index],y[index],x[first],y[first],controls[0],controls[1],controls[2],controls[3],x[last],y[last],16);
            if (distance>maximum) {
                maximum=distance;
                farthest=index;
            }
        }
        return farthest;
    }

    private boolean safeControls(double[] controls) {
        double limit=Math.max(1.0,4.0*Math.max(maximumX-minimumX,maximumY-minimumY)+4.0*tolerance);
        for (int index=0;index<controls.length;index+=2) {
            if (!Double.isFinite(controls[index])||!Double.isFinite(controls[index+1])) {
                return false;
            }
            if (controls[index]<minimumX-limit||controls[index]>maximumX+limit||controls[index+1]<minimumY-limit||controls[index+1]>maximumY+limit) {
                return false;
            }
        }
        return true;
    }

    private void appendLine(int first, int last) {
        double[] controls=lineControls(first,last);
        append(new Candidate(controls,measure(first,last,controls,FINAL_SAMPLES),first),first,last);
    }

    private void append(Candidate candidate, int first, int last) {
        if (segmentCount>=maxSegments) {
            throw new IllegalArgumentException("cubic fit exceeds the configured segment limit");
        }
        double[] segment=new double[8];
        segment[0]=x[first];
        segment[1]=y[first];
        segment[2]=candidate.controls[0];
        segment[3]=candidate.controls[1];
        segment[4]=candidate.controls[2];
        segment[5]=candidate.controls[3];
        segment[6]=x[last];
        segment[7]=y[last];
        for (int coordinate=0;coordinate<8;coordinate++) {
            if (!Double.isFinite(segment[coordinate])) {
                throw new IllegalArgumentException("cubic fit produced non-finite coordinates");
            }
        }
        result.add(segment);
        ranges.add(new int[]{first,last});
        segmentCount++;
        if (candidate.error>lastError) {
            lastError=candidate.error;
        }
    }

    private void validateResult() {
        if (result.size()!=ranges.size()||result.isEmpty()) {
            throw new IllegalArgumentException("cubic fit is incomplete");
        }
        double finalError=0.0;
        for (int index=0;index<result.size();index++) {
            double[] segment=result.get(index);
            if (!safeControls(new double[]{segment[2],segment[3],segment[4],segment[5]})) {
                throw new IllegalArgumentException("cubic fit produced unsafe control points");
            }
            if (index>0&&(!same(segment[0],segment[1],result.get(index-1)[6],result.get(index-1)[7]))) {
                throw new IllegalArgumentException("cubic fit is not continuous");
            }
            int[] range=ranges.get(index);
            double error=measure(range[0],range[1],new double[]{segment[2],segment[3],segment[4],segment[5]},FINAL_SAMPLES);
            if (error>allowedError()) {
                throw new IllegalArgumentException("cubic fit exceeds requested tolerance");
            }
            finalError=Math.max(finalError,error);
        }
        double[] last=result.get(result.size()-1);
        if (!same(last[6],last[7],result.get(0)[0],result.get(0)[1])) {
            throw new IllegalArgumentException("cubic fit does not close");
        }
        if (!exactRectilinear) {
            validateNoSelfIntersections();
        }
        lastError=tolerance==0.0?0.0:finalError;
    }

    private void validateNoSelfIntersections() {
        List<double[]> points=new ArrayList<double[]>();
        useWork((long)result.size()*33L);
        for (int index=0;index<result.size();index++) {
            double[] segment=result.get(index);
            int steps=32;
            for (int step=0;step<=steps;step++) {
                double t=step/(double)steps;
                double px=evaluate(segment[0],segment[2],segment[4],segment[6],t);
                double py=evaluate(segment[1],segment[3],segment[5],segment[7],t);
                if (points.isEmpty()||!same(points.get(points.size()-1)[0],points.get(points.size()-1)[1],px,py)) {
                    if (points.size()>=1000000) {
                        throw new IllegalArgumentException("cubic fit safety check exceeds its point limit");
                    }
                    points.add(new double[]{px,py});
                }
            }
        }
        if (points.size()>1000000) {
            throw new IllegalArgumentException("cubic fit safety check exceeds its point limit");
        }
        int count=points.size();
        if (count>1&&same(points.get(0)[0],points.get(0)[1],points.get(count-1)[0],points.get(count-1)[1])) {
            count--;
        }
        double outputArea=0.0;
        for (int index=0;index<count;index++) {
            int next=(index+1)%count;
            outputArea+=points.get(index)[0]*points.get(next)[1]-points.get(next)[0]*points.get(index)[1];
        }
        if (!Double.isFinite(outputArea)||outputArea*inputSignedArea<=0.0) {
            throw new IllegalArgumentException("cubic fit reverses ring orientation");
        }
        useWork((long)count*(count-1L)/2L);
        for (int first=0;first<count;first++) {
            int firstNext=(first+1)%count;
            for (int second=first+1;second<count;second++) {
                int secondNext=(second+1)%count;
                if (first==second||firstNext==second||secondNext==first) {
                    continue;
                }
                if (segmentsIntersect(points.get(first)[0],points.get(first)[1],points.get(firstNext)[0],points.get(firstNext)[1],points.get(second)[0],points.get(second)[1],points.get(secondNext)[0],points.get(secondNext)[1])) {
                    throw new IllegalArgumentException("cubic fit is self-intersecting");
                }
            }
        }
    }

    private double acceptanceTolerance() {
        if (tolerance==0.0) {
            return allowedError();
        }
        return tolerance*0.98;
    }

    private double allowedError() {
        return tolerance==0.0?coordinateScale*1.0e-12:tolerance;
    }

    private void useWork(long amount) {
        if (amount<0L||work>MAX_WORK-amount) {
            throw new IllegalArgumentException("cubic fit exceeds the configured work limit");
        }
        work+=amount;
    }

    private static double evaluate(double start, double control1, double control2, double end, double t) {
        double inverse=1.0-t;
        return inverse*inverse*inverse*start+3.0*inverse*inverse*t*control1+3.0*inverse*t*t*control2+t*t*t*end;
    }

    private static double pointSegmentDistance(double px, double py, double x1, double y1, double x2, double y2) {
        double dx=x2-x1;
        double dy=y2-y1;
        double lengthSquared=dx*dx+dy*dy;
        if (lengthSquared==0.0) {
            return Math.hypot(px-x1,py-y1);
        }
        double projection=((px-x1)*dx+(py-y1)*dy)/lengthSquared;
        projection=Math.max(0.0,Math.min(1.0,projection));
        double cx=x1+projection*dx;
        double cy=y1+projection*dy;
        return Math.hypot(px-cx,py-cy);
    }

    private boolean segmentsIntersect(double ax, double ay, double bx, double by, double cx, double cy, double dx, double dy) {
        double epsilon=coordinateScale*1.0e-10;
        if (Math.max(ax,bx)<Math.min(cx,dx)-epsilon||Math.max(cx,dx)<Math.min(ax,bx)-epsilon||Math.max(ay,by)<Math.min(cy,dy)-epsilon||Math.max(cy,dy)<Math.min(ay,by)-epsilon) {
            return false;
        }
        double first=cross(ax,ay,bx,by,cx,cy);
        double second=cross(ax,ay,bx,by,dx,dy);
        double third=cross(cx,cy,dx,dy,ax,ay);
        double fourth=cross(cx,cy,dx,dy,bx,by);
        if (Math.abs(first)<=epsilon&&pointOnSegment(cx,cy,ax,ay,bx,by,epsilon)||Math.abs(second)<=epsilon&&pointOnSegment(dx,dy,ax,ay,bx,by,epsilon)||Math.abs(third)<=epsilon&&pointOnSegment(ax,ay,cx,cy,dx,dy,epsilon)||Math.abs(fourth)<=epsilon&&pointOnSegment(bx,by,cx,cy,dx,dy,epsilon)) {
            return true;
        }
        return ((first>epsilon&&second<-epsilon)||(first<-epsilon&&second>epsilon))&&((third>epsilon&&fourth<-epsilon)||(third<-epsilon&&fourth>epsilon));
    }

    private boolean pointOnSegment(double px, double py, double x1, double y1, double x2, double y2, double epsilon) {
        return Math.abs(cross(x1,y1,x2,y2,px,py))<=epsilon&&px>=Math.min(x1,x2)-epsilon&&px<=Math.max(x1,x2)+epsilon&&py>=Math.min(y1,y2)-epsilon&&py<=Math.max(y1,y2)+epsilon;
    }

    private double cross(double ax, double ay, double bx, double by, double px, double py) {
        return (bx-ax)*(py-ay)-(by-ay)*(px-ax);
    }

    private static boolean same(double firstX, double firstY, double secondX, double secondY) {
        return firstX==secondX&&firstY==secondY;
    }

    private static double[] copyControls(double[] controls) {
        return new double[]{controls[0],controls[1],controls[2],controls[3]};
    }

    private static final class Candidate {
        private final double[] controls;
        private final double error;
        private int split;

        private Candidate(double[] controls, double error, int split) {
            this.controls=controls;
            this.error=error;
            this.split=split;
        }
    }
}
