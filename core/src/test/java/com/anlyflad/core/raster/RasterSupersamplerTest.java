package com.anlyflad.core.raster;

import com.anlyflad.core.model.Color;
import com.anlyflad.core.model.PathId;
import com.anlyflad.core.model.VectorPath;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

public final class RasterSupersamplerTest {
    private static final int RED=0xFFFF0000;
    private static final int BLUE=0xFF0000FF;

    @Test
    public void shouldPickScaleWithinThePixelBudget() {
        assertEquals(4,RasterSupersampler.scaleFor(1000,1000));
        assertEquals(4,RasterSupersampler.scaleFor(2048,1024));
        assertEquals(2,RasterSupersampler.scaleFor(2800,2800));
        assertEquals(1,RasterSupersampler.scaleFor(3000,3000));
        assertEquals(1,RasterSupersampler.scaleFor(10000,10000));
    }

    @Test
    public void shouldKeepUniformRegionsExactlyOnThePalette() {
        int[] pixels={RED,RED,RED,RED};
        RasterFrame frame=RasterFrame.wrap(2,2,pixels);
        RasterFrame sampled=RasterSupersampler.upsample(frame,4);
        assertEquals(8,sampled.getWidth());
        assertEquals(8,sampled.getHeight());
        for (int index=0;index<sampled.getOwnedPixels().length;index++) {
            assertEquals(RED,sampled.getOwnedPixels()[index]);
        }
    }

    @Test
    public void shouldSnapBlendedPixelsToTheNearestPaletteColor() {
        RasterFrame sampled=RasterSupersampler.upsample(RasterFrame.wrap(2,1,new int[]{RED,BLUE}),4);
        int red=0;
        int blue=0;
        for (int index=0;index<sampled.getOwnedPixels().length;index++) {
            int color=sampled.getOwnedPixels()[index];
            if (color==RED) {
                red++;
            } else if (color==BLUE) {
                blue++;
            } else {
                throw new AssertionError("unexpected interpolated color "+Integer.toHexString(color));
            }
        }
        assertEquals(16,red);
        assertEquals(16,blue);
    }

    @Test
    public void shouldSnapTransparentEdgesWithoutInventingAlphaLabels() {
        RasterFrame sampled=RasterSupersampler.upsample(RasterFrame.wrap(1,2,new int[]{0x00000000,RED}),4);
        int transparent=0;
        int red=0;
        for (int index=0;index<sampled.getOwnedPixels().length;index++) {
            int color=sampled.getOwnedPixels()[index];
            if (color==0) {
                transparent++;
            } else if (color==RED) {
                red++;
            } else {
                throw new AssertionError("unexpected interpolated color "+Integer.toHexString(color));
            }
        }
        assertEquals(16,transparent);
        assertEquals(16,red);
    }

    @Test
    public void shouldBeDeterministic() {
        RasterFrame frame=RasterFrame.wrap(3,2,new int[]{RED,BLUE,RED,BLUE,RED,BLUE});
        assertArrayEquals(RasterSupersampler.upsample(frame,4).getOwnedPixels(),RasterSupersampler.upsample(frame,4).getOwnedPixels());
    }

    @Test
    public void shouldNotReusePaletteMatchesAcrossPaletteChanges() {
        int green=0xFF00FF00;
        RasterFrame threeColors=RasterFrame.wrap(3,1,new int[]{RED,BLUE,green});
        RasterFrame sampledThree=RasterSupersampler.upsample(threeColors,4);
        for (int index=0;index<sampledThree.getOwnedPixels().length;index++) {
            int color=sampledThree.getOwnedPixels()[index];
            if (color!=RED&&color!=BLUE&&color!=green) {
                throw new AssertionError("unexpected color "+Integer.toHexString(color));
            }
        }
        RasterFrame twoColors=RasterFrame.wrap(3,1,new int[]{RED,BLUE,RED});
        RasterFrame sampledTwo=RasterSupersampler.upsample(twoColors,4);
        for (int index=0;index<sampledTwo.getOwnedPixels().length;index++) {
            int color=sampledTwo.getOwnedPixels()[index];
            if (color!=RED&&color!=BLUE) {
                throw new AssertionError("unexpected color "+Integer.toHexString(color));
            }
        }
    }

    @Test
    public void shouldSkipFramesThatExceedThePaletteLimit() {
        int[] pixels=new int[257];
        for (int index=0;index<pixels.length;index++) {
            pixels[index]=0xFF000000|index;
        }
        RasterFrame frame=RasterFrame.wrap(257,1,pixels);
        assertSame(frame,RasterSupersampler.upsample(frame,4));
        assertSame(frame,RasterSupersampler.sample(frame));
    }

    @Test
    public void shouldScalePathsBackIntoSourceCoordinates() {
        VectorPath path=new VectorPath(PathId.of(3),Collections.singletonList(new double[]{0.0,0.0,8.0,0.0,8.0,8.0,0.0,8.0}),new Color(10,20,30,255),0.5,VectorPath.FillRule.EVEN_ODD,64.0);
        List<VectorPath> scaled=RasterSupersampler.scaleBack(Collections.singletonList(path),4);
        VectorPath result=scaled.get(0);
        assertEquals(path.getId(),result.getId());
        assertEquals(path.getFill(),result.getFill());
        assertEquals(0.5,result.getOpacity(),0.0);
        assertEquals(VectorPath.FillRule.EVEN_ODD,result.getFillRule());
        assertArrayEquals(new double[]{0.0,0.0,2.0,0.0,2.0,2.0,0.0,2.0},result.getRing(0),0.0);
        assertEquals(4.0,result.getArea(),0.0);
    }

    @Test
    public void shouldRejectInvalidScaleAndFrame() {
        assertThrows(IllegalArgumentException.class,() -> RasterSupersampler.upsample(null,4));
        assertThrows(IllegalArgumentException.class,() -> RasterSupersampler.upsample(RasterFrame.wrap(1,1,new int[]{RED}),0));
        assertThrows(IllegalArgumentException.class,() -> RasterSupersampler.upsample(RasterFrame.wrap(1,1,new int[]{RED}),5));
        assertThrows(IllegalArgumentException.class,() -> RasterSupersampler.sample(null));
    }
}
