package com.vectorium.core.raster;

import com.vectorium.core.model.VectorPath;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class ColorCurveVectorizerTest {
    private static final int RED=0xFFFF0000;

    @Test
    public void shouldRetainFallbackRingsAndAddCurves() {
        int width=6;
        int height=6;
        int[] pixels=new int[width*height];
        for (int y=1;y<5;y++) {
            for (int x=1;x<5;x++) {
                pixels[y*width+x]=RED;
            }
        }
        List<VectorPath> paths=ColorCurveVectorizer.vectorize(RasterFrame.wrap(width,height,pixels),0.5);
        assertEquals(1,paths.size());
        VectorPath path=paths.get(0);
        assertTrue(path.hasCubicData());
        assertEquals(1,path.getRingCount());
        assertEquals(4,path.getNodeCount());
        assertEquals(path.getRing(0)[0],path.getCubicRing(0)[0],0.0);
        assertEquals(path.getRing(0)[1],path.getCubicRing(0)[1],0.0);
        assertEquals(16.0,path.getArea(),0.0);
        boolean curved=false;
        for (int index=2;index<path.getCubicRing(0).length;index+=2) {
            if (path.getCubicRing(0)[index]!=path.getCubicRing(0)[index-1]) {
                curved=true;
            }
        }
        assertTrue(curved);
    }

    @Test
    public void shouldFitOuterAndInnerRingsWithoutDroppingHoles() {
        int[] pixels=new int[25];
        for (int y=0;y<5;y++) {
            for (int x=0;x<5;x++) {
                if (x==0||x==4||y==0||y==4) {
                    pixels[y*5+x]=RED;
                }
            }
        }
        VectorPath path=ColorCurveVectorizer.vectorize(5,5,pixels,0.5).get(0);
        assertEquals(2,path.getRingCount());
        assertEquals(2,path.getCubicRingCount());
        assertEquals(16.0,path.getArea(),0.0);
        assertEquals(path.getRing(0)[0],path.getCubicRing(0)[0],0.0);
        assertEquals(path.getRing(0)[1],path.getCubicRing(0)[1],0.0);
    }

    @Test
    public void shouldNotAbortRasterCurveModeWhenGeneralFitterRejects() {
        int[] pixels={RED,RED,RED,RED,0,RED,RED,RED,0};
        List<VectorPath> paths=assertDoesNotThrow(() -> ColorCurveVectorizer.vectorize(3,3,pixels,0.0));
        assertEquals(1,paths.size());
        assertTrue(paths.get(0).hasCubicData());
    }

    @Test
    public void shouldReduceRasterCommandsAndRoundRetainedCorners() {
        int width=160;
        int height=32;
        int[] pixels=new int[width*height];
        for (int y=0;y<height;y++) {
            for (int x=0;x<width;x++) {
                int top=x%2==0?3:4;
                if ((x>=12&&x<22)||(x>=50&&x<60)||(x>=95&&x<105)||(x>=135&&x<145)) {
                    top=8;
                }
                if (y>=top&&y<height-3) {
                    pixels[y*width+x]=RED;
                }
            }
        }
        for (int y=12;y<17;y++) {
            for (int x=70;x<74;x++) {
                pixels[y*width+x]=0;
            }
        }
        VectorPath contour=ColorContourVectorizer.vectorize(width,height,pixels).get(0);
        VectorPath curved=ColorCurveVectorizer.vectorize(width,height,pixels).get(0);
        assertEquals(2,curved.getRingCount());
        assertTrue(curved.getCubicSegmentCount()*3<contour.getNodeCount());
        assertTrue(hasNonCollinearCubic(curved));
    }

    @Test
    public void shouldRejectInvalidTolerance() {
        assertThrows(IllegalArgumentException.class,() -> ColorCurveVectorizer.vectorize(1,1,new int[]{RED},Double.NaN));
    }

    private static boolean hasNonCollinearCubic(VectorPath path) {
        for (double[] segment:path.getCubicSegments()) {
            double firstX=segment[2]-segment[0];
            double firstY=segment[3]-segment[1];
            double secondX=segment[4]-segment[2];
            double secondY=segment[5]-segment[3];
            if (Math.abs(firstX*secondY-firstY*secondX)>1.0e-9) {
                return true;
            }
        }
        return false;
    }
}
