package com.anlyflad.core.raster;

import com.anlyflad.core.model.Color;
import com.anlyflad.core.model.VectorPath;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
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
        assertEquals(16.0,path.getArea(),0.5);
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
        assertEquals(16.0,path.getArea(),0.5);
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
        assertTrue(curved.getCubicSegmentCount()<contour.getNodeCount());
        assertTrue(hasNonCollinearCubic(curved));
    }

    @Test
    public void shouldRejectInvalidTolerance() {
        assertThrows(IllegalArgumentException.class,() -> ColorCurveVectorizer.vectorize(1,1,new int[]{RED},Double.NaN));
    }

    @Test
    public void shouldProduceIdenticalGeometryInAnyTaskOrder() {
        int width=24;
        int height=24;
        int[] pixels=new int[width*height];
        for (int y=0;y<height;y++) {
            for (int x=0;x<width;x++) {
                if ((x/4+y/4)%2==0) {
                    pixels[y*width+x]=RED;
                }
            }
        }
        List<VectorPath> forward=ColorCurveVectorizer.vectorize(width,height,pixels,0.5);
        ColorCurveVectorizer.setParallelRunner(new ParallelRunner() {
            public void run(int taskCount,Task task) {
                for (int index=taskCount-1;index>=0;index--) {
                    task.run(index);
                }
            }
        });
        try {
            List<VectorPath> reverse=ColorCurveVectorizer.vectorize(width,height,pixels,0.5);
            assertGeometryEquals(forward,reverse);
        } finally {
            ColorCurveVectorizer.setParallelRunner(null);
        }
    }

    private static void assertGeometryEquals(List<VectorPath> first,List<VectorPath> second) {
        assertEquals(first.size(),second.size());
        for (int pathIndex=0;pathIndex<first.size();pathIndex++) {
            VectorPath firstPath=first.get(pathIndex);
            VectorPath secondPath=second.get(pathIndex);
            assertEquals(firstPath.getRingCount(),secondPath.getRingCount());
            for (int ringIndex=0;ringIndex<firstPath.getRingCount();ringIndex++) {
                assertArrayEquals(firstPath.getRing(ringIndex),secondPath.getRing(ringIndex),0.0);
            }
            double[][] firstCubic=firstPath.getCubicRingCoordinates();
            double[][] secondCubic=secondPath.getCubicRingCoordinates();
            assertEquals(firstCubic.length,secondCubic.length);
            for (int ringIndex=0;ringIndex<firstCubic.length;ringIndex++) {
                assertArrayEquals(firstCubic[ringIndex],secondCubic[ringIndex],0.0);
            }
        }
    }

    @Test
    public void shouldFallBackToPolylineWhenFitterRejectsSelfIntersectingRing() {
        int[] pixels={
            0xFF00FF00,0xFF0000FF,0xFF0000FF,0xFF00FF00,0xFF00FF00,0xFFFF0000,0xFF00FF00,
            0xFF00FF00,0xFF00FF00,0xFF00FF00,0xFF00FF00,0xFF00FF00,0xFF00FF00,0xFFFF0000,
            0xFF00FF00,0xFFFF0000,0xFF00FF00,0xFF0000FF,0xFF00FF00,0xFF0000FF,0xFFFF0000,
            0xFFFF0000,0xFF00FF00,0xFF00FF00,0xFF00FF00,0xFFFF0000,0xFF0000FF,0xFFFF0000,
            0xFF00FF00,0xFF00FF00,0xFFFF0000,0xFF00FF00,0xFF00FF00,0xFFFF0000,0xFF0000FF,
            0xFF0000FF,0xFF0000FF,0xFF00FF00,0xFF00FF00,0xFF00FF00,0xFF00FF00,0xFF00FF00,
            0xFF0000FF,0xFF0000FF,0xFF0000FF,0xFF00FF00,0xFFFF0000,0xFF00FF00,0xFFFF0000
        };
        List<VectorPath> paths=assertDoesNotThrow(() -> ColorCurveVectorizer.vectorize(7,7,pixels,0.0));
        assertTrue(paths.size()>0);
        for (int pathIndex=0;pathIndex<paths.size();pathIndex++) {
            double[][] rings=paths.get(pathIndex).getRingCoordinates();
            assertTrue(rings.length>0);
            for (int ringIndex=0;ringIndex<rings.length;ringIndex++) {
                assertTrue(rings[ringIndex].length>=6);
            }
        }
    }

    @Test
    public void shouldKeepSubpixelGeometryAndCrackFreeCoverage() {
        int width=64;
        int height=64;
        int[] pixels=new int[width*height];
        for (int y=0;y<height;y++) {
            for (int x=0;x<width;x++) {
                pixels[y*width+x]=x<=y?RED:0xFF0000FF;
            }
        }
        List<VectorPath> paths=ColorCurveVectorizer.vectorize(width,height,pixels,0.5);
        assertEquals(2,paths.size());
        assertTrue(hasFractionalCoordinate(paths));
        assertEquals(0,countUncoveredPixels(paths,width,height,4));
    }

    @Test
    public void shouldVectorizeShapeIndependentlyOfSourceResolution() {
        int size=24;
        int[] base=new int[size*size];
        for (int y=0;y<size;y++) {
            for (int x=0;x<size;x++) {
                base[y*size+x]=x>=y?RED:0xFF0000FF;
            }
        }
        int factor=4;
        int large=size*factor;
        int[] upscaled=new int[large*large];
        for (int y=0;y<large;y++) {
            for (int x=0;x<large;x++) {
                upscaled[y*large+x]=base[(y/factor)*size+x/factor];
            }
        }
        List<VectorPath> basePaths=ColorCurveVectorizer.vectorize(size,size,base,0.5);
        List<VectorPath> scaledPaths=ColorCurveVectorizer.vectorize(large,large,upscaled,0.5);
        assertEquals(basePaths.size(),scaledPaths.size());
        for (int index=0;index<basePaths.size();index++) {
            assertEquals(basePaths.get(index).getRingCount(),scaledPaths.get(index).getRingCount());
            assertEquals(basePaths.get(index).getFill().toArgb(),scaledPaths.get(index).getFill().toArgb());
            assertMatchingBounds(basePaths.get(index).getRingCoordinates(),scaledPaths.get(index).getRingCoordinates(),factor);
        }
    }

    private static boolean hasFractionalCoordinate(List<VectorPath> paths) {
        for (int pathIndex=0;pathIndex<paths.size();pathIndex++) {
            double[][] rings=paths.get(pathIndex).getRingCoordinates();
            for (int ringIndex=0;ringIndex<rings.length;ringIndex++) {
                for (int index=0;index<rings[ringIndex].length;index++) {
                    if (rings[ringIndex][index]!=Math.rint(rings[ringIndex][index])) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private static void assertMatchingBounds(double[][] baseRings,double[][] scaledRings,int factor) {
        assertEquals(baseRings.length,scaledRings.length);
        for (int ringIndex=0;ringIndex<baseRings.length;ringIndex++) {
            double[] baseBounds=bounds(baseRings[ringIndex]);
            double[] scaledBounds=bounds(scaledRings[ringIndex]);
            for (int coordinate=0;coordinate<4;coordinate++) {
                assertEquals(baseBounds[coordinate],scaledBounds[coordinate]/factor,0.75);
            }
        }
    }

    private static double[] bounds(double[] coordinates) {
        double minimumX=coordinates[0];
        double minimumY=coordinates[1];
        double maximumX=coordinates[0];
        double maximumY=coordinates[1];
        for (int index=2;index<coordinates.length;index+=2) {
            minimumX=Math.min(minimumX,coordinates[index]);
            minimumY=Math.min(minimumY,coordinates[index+1]);
            maximumX=Math.max(maximumX,coordinates[index]);
            maximumY=Math.max(maximumY,coordinates[index+1]);
        }
        return new double[]{minimumX,minimumY,maximumX,maximumY};
    }

    private static int countUncoveredPixels(List<VectorPath> paths,int width,int height,int scale) {
        BufferedImage image=new BufferedImage(width*scale,height*scale,BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics=image.createGraphics();
        try {
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_OFF);
            graphics.scale(scale,scale);
            for (int index=0;index<paths.size();index++) {
                VectorPath path=paths.get(index);
                Path2D.Double shape=toShape(path);
                graphics.setColor(new java.awt.Color(path.getFill().toArgb(),true));
                graphics.fill(shape);
                graphics.setStroke(new java.awt.BasicStroke(1.0f));
                graphics.draw(shape);
            }
        } finally {
            graphics.dispose();
        }
        int uncovered=0;
        for (int y=0;y<image.getHeight();y++) {
            for (int x=0;x<image.getWidth();x++) {
                if ((image.getRGB(x,y)>>>24)==0) {
                    uncovered++;
                }
            }
        }
        return uncovered;
    }

    private static Path2D.Double toShape(VectorPath path) {
        Path2D.Double shape=new Path2D.Double(Path2D.WIND_EVEN_ODD);
        List<double[]> rings=path.getRings();
        double[][] cubicRings=path.getCubicRingCoordinates();
        for (int ringIndex=0;ringIndex<rings.size();ringIndex++) {
            if (ringIndex<cubicRings.length&&cubicRings[ringIndex]!=null) {
                double[] segments=cubicRings[ringIndex];
                shape.moveTo(segments[0],segments[1]);
                for (int offset=0;offset<segments.length;offset+=8) {
                    shape.curveTo(segments[offset+2],segments[offset+3],segments[offset+4],segments[offset+5],segments[offset+6],segments[offset+7]);
                }
                shape.closePath();
            } else {
                double[] coordinates=rings.get(ringIndex);
                shape.moveTo(coordinates[0],coordinates[1]);
                for (int index=2;index<coordinates.length;index+=2) {
                    shape.lineTo(coordinates[index],coordinates[index+1]);
                }
                shape.closePath();
            }
        }
        return shape;
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
