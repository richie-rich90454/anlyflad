package com.anlyflad.core.svg;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import com.anlyflad.core.model.Color;
import com.anlyflad.core.model.VectorPath;
public final class SvgPathParserTest {
    @Test
    public void shouldParseImplicitCommandsAndCloseExactly() throws Exception {
        List<VectorPath> paths=new SvgPathParser().parse("M0 0 10 0 10 10 z", new Color(1, 2, 3, 255), 0.5);
        assertEquals(1, paths.size());
        VectorPath path=paths.get(0);
        assertTrue(path.isClosed());
        assertEquals(4, path.getNodeCount());
        assertEquals(0.0, path.getCoordinates()[0], 0.0);
        assertEquals(0.0, path.getCoordinates()[1], 0.0);
        assertEquals(0.0, path.getCoordinates()[6], 0.0);
        assertEquals(0.0, path.getCoordinates()[7], 0.0);
        assertEquals(new Color(1, 2, 3, 255), path.getFill());
        assertEquals(0.5, path.getOpacity(), 0.0);
    }
    @Test
    public void shouldSupportAllRelativeCommandFamilies() throws Exception {
        List<VectorPath> paths=new SvgPathParser().parse("m1 1 l2 0 h1 v1 c1 0 2 1 3 1 s1 0 2 0 q1 1 2 0 t2 0 a1 1 0 0 1 2 0 z", new Color(0, 0, 0, 255), 1.0);
        assertEquals(1, paths.size());
        VectorPath path=paths.get(0);
        assertTrue(path.isClosed());
        assertTrue(path.getNodeCount()>=10);
        assertEquals(1.0, path.getCoordinates()[0], 0.0);
        assertEquals(1.0, path.getCoordinates()[1], 0.0);
    }
    @Test
    public void shouldFlattenCubicAndQuadraticCurvesWithFixedSteps() throws Exception {
        List<VectorPath> cubic=new SvgPathParser().parse("M0 0 C0 0 0 0 0 0");
        assertEquals(5, cubic.get(0).getNodeCount());
        List<VectorPath> quadratic=new SvgPathParser().parse("M0 0 Q10 10 20 0 T40 0");
        assertTrue(quadratic.get(0).getNodeCount()>=9);
        assertEquals(40.0, quadratic.get(0).getCoordinates()[quadratic.get(0).getCoordinates().length-2], 1.0e-9);
    }
    @Test
    public void shouldParseSubpathsAndApplyTransform() throws Exception {
        List<VectorPath> paths=new SvgPathParser().parse("M0 0 L1 1 M2 2 L3 3", SvgTransform.translation(10.0, 20.0), new Color(0, 0, 0, 255), 1.0);
        assertEquals(2, paths.size());
        assertEquals(0, paths.get(0).getId().getValue());
        assertEquals(1, paths.get(1).getId().getValue());
        assertEquals(10.0, paths.get(0).getCoordinates()[0], 0.0);
        assertEquals(22.0, paths.get(1).getCoordinates()[1], 0.0);
        assertFalse(paths.get(0).isClosed());
    }
    @Test
    public void shouldAcceptExponentsSignsAndCommas() throws Exception {
        List<VectorPath> paths=new SvgPathParser().parse("M1e1,-2 L+3.5-4 L.5.25");
        assertEquals(1, paths.size());
        assertEquals(3, paths.get(0).getNodeCount());
        assertEquals(10.0, paths.get(0).getCoordinates()[0], 0.0);
        assertEquals(-2.0, paths.get(0).getCoordinates()[1], 0.0);
    }
    @Test
    public void shouldRejectMalformedPathNumbers() {
        String[] invalid=new String[]{"M0", "M0 0 L1 nope", "M0 0 A1 1 0 2 1 2 2", "M0 0 L1e 2"};
        for (int index=0;index<invalid.length;index++) {
            try {
                new SvgPathParser().parse(invalid[index]);
                fail("Expected malformed path to be rejected");
            } catch (SvgParseException exception) {
                assertTrue(exception.getMessage().length()>0);
            }
        }
    }
    @Test
    public void shouldRetainTransformedCubicDataAndContinuousClosure() throws Exception {
        List<VectorPath> paths=new SvgPathParser().parse("M0 0 L1 0 C2 1 3 2 4 3 Z", SvgTransform.translation(10.0, 20.0), new Color(0, 0, 0, 255), 1.0);
        VectorPath path=paths.get(0);
        assertTrue(path.isClosed());
        assertTrue(path.hasCubicData());
        assertEquals(3, path.getCubicSegmentCount());
        double[] cubic=path.getCubicRing(0);
        assertEquals(10.0, cubic[0], 0.0);
        assertEquals(20.0, cubic[1], 0.0);
        assertEquals(12.0, cubic[10], 0.0);
        assertEquals(21.0, cubic[11], 0.0);
        assertEquals(13.0, cubic[12], 0.0);
        assertEquals(22.0, cubic[13], 0.0);
        assertEquals(14.0, cubic[14], 0.0);
        assertEquals(23.0, cubic[15], 0.0);
        double[] fallback=path.getCoordinates();
        assertEquals(cubic[0], fallback[0], 0.0);
        assertEquals(cubic[1], fallback[1], 0.0);
        assertEquals(cubic[cubic.length-2], fallback[fallback.length-2], 0.0);
        assertEquals(cubic[cubic.length-1], fallback[fallback.length-1], 0.0);
        for (int offset=8;offset<cubic.length;offset+=8) {
            assertEquals(cubic[offset-2], cubic[offset], 0.0);
            assertEquals(cubic[offset-1], cubic[offset+1], 0.0);
        }
    }
    @Test
    public void shouldConvertQuadraticSmoothAndCompactArcFlagsToCubicData() throws Exception {
        List<VectorPath> paths=new SvgPathParser().parse("M0 0 Q10 10 20 0 T40 0 A10 10 0 0110 10");
        VectorPath path=paths.get(0);
        assertTrue(path.hasCubicData());
        assertTrue(path.getCubicSegmentCount()>=4);
        assertEquals(20.0, path.getCubicSegment(0, 0)[6], 0.0);
        assertEquals(40.0, path.getCubicSegment(0, 1)[6], 0.0);
        assertEquals(10.0, path.getCubicSegment(0, path.getCubicSegmentCount()-1)[6], 0.0);
        assertEquals(10.0, path.getCoordinates()[path.getCoordinates().length-2], 0.0);
    }
    @Test
    public void shouldRejectDegenerateCompoundSubpaths() throws Exception {
        try {
            new SvgPathParser().parseCompound("M0 0 M1 1", SvgTransform.identity(), new Color(0, 0, 0, 255), 1.0, VectorPath.FillRule.NONZERO);
            fail("Expected degenerate compound geometry to be rejected");
        } catch (SvgParseException exception) {
            assertTrue(exception.getMessage().length()>0);
        }
    }
}
