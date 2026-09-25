package com.vectorium.core.geometry;

import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class CubicBezierFitterTest {
    @Test
    public void shouldFitClosedRingWithinToleranceAndKeepClosure() {
        double[] ring={0.0,0.0,10.0,0.0,10.0,10.0,0.0,10.0};
        CubicBezierFitter fitter=new CubicBezierFitter(0.25);
        List<double[]> segments=fitter.fit(ring);
        assertTrue(segments.size()>=3);
        assertEquals(ring[0],segments.get(0)[0],0.0);
        assertEquals(ring[1],segments.get(0)[1],0.0);
        double[] last=segments.get(segments.size()-1);
        assertEquals(ring[0],last[6],0.0);
        assertEquals(ring[1],last[7],0.0);
        assertTrue(fitter.getLastError()<=0.25);
        boolean curved=false;
        for (double[] segment:segments) {
            if (Math.abs(segment[1]-segment[3])>1.0e-9||Math.abs(segment[3]-segment[5])>1.0e-9) {
                curved=true;
            }
        }
        assertTrue(curved);
    }

    @Test
    public void shouldUseExactLineFallbackAtZeroTolerance() {
        CubicBezierFitter fitter=new CubicBezierFitter(0.0);
        List<double[]> segments=fitter.fit(new double[]{0.0,0.0,10.0,0.0,10.0,10.0,0.0,10.0});
        assertEquals(4,segments.size());
        assertEquals(0.0,fitter.getLastError(),0.0);
    }

    @Test
    public void shouldKeepSampledCircleWithinTolerance() {
        double[] ring=new double[64*2];
        for (int index=0;index<64;index++) {
            double angle=2.0*Math.PI*index/64.0;
            ring[index*2]=100.0+50.0*Math.cos(angle);
            ring[index*2+1]=100.0+50.0*Math.sin(angle);
        }
        CubicBezierFitter fitter=new CubicBezierFitter(0.05);
        List<double[]> segments=fitter.fit(ring);
        assertTrue(segments.size()>0);
        assertTrue(fitter.getLastError()<=0.05);
        for (double[] segment:segments) {
            for (int sample=1;sample<1000;sample++) {
                double t=sample/1000.0;
                double px=bezier(segment[0],segment[2],segment[4],segment[6],t);
                double py=bezier(segment[1],segment[3],segment[5],segment[7],t);
                assertTrue(distanceToRing(px,py,ring)<=0.0500001);
            }
        }
    }

    @Test
    public void shouldFitLargeRectilinearRingsWithoutQuadraticValidation() {
        int teeth=200;
        double[] ring=new double[(teeth*2+3)*2];
        int offset=0;
        ring[offset++]=0.0;
        ring[offset++]=0.0;
        for (int index=0;index<teeth;index++) {
            ring[offset++]=index;
            ring[offset++]=index+1.0;
            ring[offset++]=index+1.0;
            ring[offset++]=index+1.0;
        }
        ring[offset++]=teeth;
        ring[offset++]=teeth+1.0;
        ring[offset++]=0.0;
        ring[offset++]=teeth+1.0;
        CubicBezierFitter fitter=new CubicBezierFitter(0.5);
        List<double[]> segments=fitter.fit(ring);
        assertEquals(teeth*2+3,segments.size());
        assertTrue(fitter.getLastError()<=1.0e-12);
    }
    @Test
    public void shouldRejectDegenerateAndSelfIntersectingRings() {
        assertThrows(IllegalArgumentException.class,() -> CubicBezierFitter.fit(new double[]{0.0,0.0,1.0,0.0,2.0,0.0},0.1));
        assertThrows(IllegalArgumentException.class,() -> CubicBezierFitter.fit(new double[]{0.0,0.0,10.0,10.0,0.0,10.0,10.0,0.0},0.1));
    }

    private static double distanceToRing(double px, double py, double[] ring) {
        double minimum=Double.POSITIVE_INFINITY;
        for (int index=0;index<ring.length/2;index++) {
            int next=(index+1)%(ring.length/2);
            double x1=ring[index*2];
            double y1=ring[index*2+1];
            double x2=ring[next*2];
            double y2=ring[next*2+1];
            double dx=x2-x1;
            double dy=y2-y1;
            double length=dx*dx+dy*dy;
            double t=length==0.0?0.0:((px-x1)*dx+(py-y1)*dy)/length;
            t=Math.max(0.0,Math.min(1.0,t));
            minimum=Math.min(minimum,Math.hypot(px-(x1+t*dx),py-(y1+t*dy)));
        }
        return minimum;
    }

    private static double bezier(double start, double first, double second, double end, double t) {
        double inverse=1.0-t;
        return inverse*inverse*inverse*start+3.0*inverse*inverse*t*first+3.0*inverse*t*t*second+t*t*t*end;
    }
}
