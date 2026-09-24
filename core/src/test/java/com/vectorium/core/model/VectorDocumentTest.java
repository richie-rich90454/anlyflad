package com.vectorium.core.model;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
public final class VectorDocumentTest {
    @Test
    public void shouldStoreCopiedImmutablePathsAndDirectOwnedPixels() {
        VectorPath firstPath=new VectorPath(PathId.zero(), new double[]{0.0, 0.0, 1.0, 1.0}, false, new Color(1, 2, 3, 255), 1.0);
        List<VectorPath> sourcePaths=new ArrayList<VectorPath>();
        sourcePaths.add(firstPath);
        int[] sourcePixels={1, 2, 3, 4};
        VectorDocument document=new VectorDocument("document", new SvgOrigin("drawing.svg"), sourcePaths, 2, 2, sourcePixels);
        sourcePaths.clear();
        sourcePixels[0]=99;
        assertEquals(1, document.getPaths().size());
        assertEquals(firstPath, document.getPaths().get(0));
        assertSame(document.getOwnedPixels(), document.getOwnedPixels());
        assertArrayEquals(new int[]{1, 2, 3, 4}, document.getOwnedPixels());
        try {
            document.getPaths().add(firstPath);
            fail("Expected paths list to be unmodifiable");
        } catch (UnsupportedOperationException exception) {
            assertEquals(UnsupportedOperationException.class, exception.getClass());
        }
    }
    @Test
    public void shouldCreateEmptySvgAndRasterDocuments() {
        VectorDocument svg=VectorDocument.emptySvg("svg-document", 2, 3);
        VectorDocument raster=VectorDocument.emptyRaster("raster-document", 1, 2);
        assertEquals(new SvgOrigin("svg-document"), svg.getOrigin());
        assertFalse(svg.getOrigin().isRaster());
        assertEquals(6, svg.getOwnedPixels().length);
        assertEquals(new RasterOrigin("raster-document"), raster.getOrigin());
        assertTrue(raster.getOrigin().isRaster());
        assertEquals(2, raster.getOwnedPixels().length);
    }
    @Test
    public void shouldTransformPathsPixelsSizeAndOrigin() {
        VectorPath originalPath=new VectorPath(PathId.zero(), new double[]{0.0, 0.0, 1.0, 0.0, 1.0, 1.0}, true, new Color(0, 0, 0, 255), 1.0);
        VectorPath replacementPath=new VectorPath(PathId.of(1), new double[]{0.0, 0.0, 2.0, 0.0, 2.0, 2.0, 0.0, 2.0}, true, new Color(0, 0, 0, 255), 1.0);
        VectorDocument original=new VectorDocument("document", new SvgOrigin("drawing.svg"), Arrays.asList(originalPath), 2, 2, new int[]{1, 2, 3, 4});
        List<VectorPath> replacementPaths=new ArrayList<VectorPath>();
        replacementPaths.add(originalPath);
        replacementPaths.add(replacementPath);
        VectorDocument withPaths=original.withPaths(replacementPaths);
        VectorDocument withPixels=original.withPixels(new int[]{5, 6, 7, 8});
        VectorDocument withSize=original.withSize(3, 2);
        VectorDocument withOrigin=original.withOrigin(new RasterOrigin("drawing.png"));
        replacementPaths.clear();
        assertEquals(2, withPaths.getPaths().size());
        assertArrayEquals(new int[]{5, 6, 7, 8}, withPixels.getOwnedPixels());
        assertEquals(3, withSize.getWidth());
        assertEquals(2, withSize.getHeight());
        assertArrayEquals(new int[]{1, 2, 3, 4, 0, 0}, withSize.getOwnedPixels());
        assertEquals(new RasterOrigin("drawing.png"), withOrigin.getOrigin());
        assertEquals(1, original.getPaths().size());
        assertArrayEquals(new int[]{1, 2, 3, 4}, original.getOwnedPixels());
        assertEquals(2, original.getWidth());
    }
    @Test
    public void shouldShrinkOwnedPixelsWhenSizeShrinks() {
        VectorDocument original=VectorDocument.emptyRaster("document", 3, 2);
        original.getOwnedPixels()[0]=7;
        VectorDocument resized=original.withSize(1, 1);
        assertEquals(1, resized.getOwnedPixels().length);
        assertEquals(7, resized.getOwnedPixels()[0]);
        assertEquals(6, original.getOwnedPixels().length);
    }
    @Test
    public void shouldRejectInvalidDocumentArguments() {
        int[] invalidSizes={-1, Integer.MIN_VALUE};
        for (int size : invalidSizes) {
            try {
                new VectorDocument("document", new SvgOrigin("drawing.svg"), new ArrayList<VectorPath>(), size, 0, new int[0]);
                fail("Expected invalid width to be rejected");
            } catch (IllegalArgumentException exception) {
                assertEquals(true, exception.getMessage().length()>0);
            }
            try {
                new VectorDocument("document", new SvgOrigin("drawing.svg"), new ArrayList<VectorPath>(), 0, size, new int[0]);
                fail("Expected invalid height to be rejected");
            } catch (IllegalArgumentException exception) {
                assertEquals(true, exception.getMessage().length()>0);
            }
        }
        try {
            new VectorDocument(" ", new SvgOrigin("drawing.svg"), new ArrayList<VectorPath>(), 0, 0, new int[0]);
            fail("Expected blank document id to be rejected");
        } catch (IllegalArgumentException exception) {
            assertEquals(true, exception.getMessage().length()>0);
        }
        try {
            new VectorDocument("document", null, new ArrayList<VectorPath>(), 0, 0, new int[0]);
            fail("Expected null origin to be rejected");
        } catch (IllegalArgumentException exception) {
            assertEquals(true, exception.getMessage().length()>0);
        }
        try {
            new VectorDocument("document", new SvgOrigin("drawing.svg"), null, 0, 0, new int[0]);
            fail("Expected null paths to be rejected");
        } catch (IllegalArgumentException exception) {
            assertEquals(true, exception.getMessage().length()>0);
        }
        List<VectorPath> pathsWithNull=new ArrayList<VectorPath>();
        pathsWithNull.add(null);
        try {
            new VectorDocument("document", new SvgOrigin("drawing.svg"), pathsWithNull, 0, 0, new int[0]);
            fail("Expected null path to be rejected");
        } catch (IllegalArgumentException exception) {
            assertEquals(true, exception.getMessage().length()>0);
        }
        try {
            new VectorDocument("document", new SvgOrigin("drawing.svg"), new ArrayList<VectorPath>(), 1, 1, new int[0]);
            fail("Expected mismatched pixel buffer to be rejected");
        } catch (IllegalArgumentException exception) {
            assertEquals(true, exception.getMessage().length()>0);
        }
        try {
            new VectorDocument("document", new SvgOrigin("drawing.svg"), new ArrayList<VectorPath>(), 0, 0, null);
            fail("Expected null pixel buffer to be rejected");
        } catch (IllegalArgumentException exception) {
            assertEquals(true, exception.getMessage().length()>0);
        }
    }
    @Test
    public void shouldUseValueEqualityHashCodeAndString() {
        VectorPath path=new VectorPath(PathId.zero(), new double[]{0.0, 0.0, 1.0, 0.0, 1.0, 1.0}, true, new Color(0, 0, 0, 255), 1.0);
        VectorDocument first=new VectorDocument("document", new SvgOrigin("drawing.svg"), Arrays.asList(path), 1, 1, new int[]{123});
        VectorDocument second=new VectorDocument("document", new SvgOrigin("drawing.svg"), Arrays.asList(path), 1, 1, new int[]{123});
        assertEquals(first, second);
        assertEquals(first.hashCode(), second.hashCode());
        assertNotEquals(first, second.withOrigin(new RasterOrigin("drawing.svg")));
        assertEquals("VectorDocument{documentId=document, origin=SvgOrigin{sourceName=drawing.svg}, paths=[VectorPath{id=PathId{value=0}, coordinates=[0.0, 0.0, 1.0, 0.0, 1.0, 1.0], closed=true, fill=Color{red=0, green=0, blue=0, alpha=255}, opacity=1.0, bounds=Rect{minX=0.0, minY=0.0, maxX=1.0, maxY=1.0}, area=0.5, nodeCount=3}], width=1, height=1, pixelLength=1}", first.toString());
    }
}
