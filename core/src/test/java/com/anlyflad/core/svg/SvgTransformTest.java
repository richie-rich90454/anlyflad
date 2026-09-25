package com.anlyflad.core.svg;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import com.anlyflad.core.model.Point;
public final class SvgTransformTest {
    @Test
    public void shouldCreateIdentityAndBasicFactories() {
        SvgTransform identity=SvgTransform.identity();
        assertEquals(new Point(3.0, 4.0), identity.transform(3.0, 4.0));
        assertEquals(new Point(11.0, 14.0), SvgTransform.translation(10.0, 10.0).transform(1.0, 4.0));
        assertEquals(new Point(6.0, 12.0), SvgTransform.scale(2.0, 3.0).transform(3.0, 4.0));
        assertEquals(0.0, SvgTransform.rotation(90.0).transform(1.0, 0.0).getX(), 1.0e-12);
        assertEquals(1.0, SvgTransform.rotation(90.0).transform(1.0, 0.0).getY(), 1.0e-12);
        assertEquals(new Point(11.0, 1.0), SvgTransform.skewX(45.0).transform(10.0, 1.0));
        Point skewed=SvgTransform.skewY(45.0).transform(1.0, 10.0);
        assertEquals(1.0, skewed.getX(), 0.0);
        assertEquals(11.0, skewed.getY(), 1.0e-12);
        assertEquals(new SvgTransform(1.0, 2.0, 3.0, 4.0, 5.0, 6.0), SvgTransform.matrix(1.0, 2.0, 3.0, 4.0, 5.0, 6.0));
    }
    @Test
    public void shouldConcatenateInSvgOrder() {
        SvgTransform transform=SvgTransform.translation(10.0, 20.0).concatenate(SvgTransform.scale(2.0, 3.0));
        assertEquals(new Point(12.0, 26.0), transform.transform(1.0, 2.0));
        assertEquals(new Point(14.0, 38.0), transform.concat(SvgTransform.scale(2.0, 3.0)).transform(1.0, 2.0));
    }
    @Test
    public void shouldParseTransformListsAndRotationCenters() throws Exception {
        SvgTransform transform=SvgTransform.parse("translate(1 2) scale(2)");
        assertEquals(new Point(3.0, 6.0), transform.transform(1.0, 2.0));
        SvgTransform centered=SvgTransform.parse("rotate(90 1 1)");
        assertEquals(new Point(1.0, 2.0), centered.transform(2.0, 1.0));
        assertEquals(SvgTransform.identity(), SvgTransform.parse(""));
    }
    @Test
    public void shouldRejectNonFiniteAndMalformedTransforms() {
        try {
            new SvgTransform(Double.NaN, 0.0, 0.0, 1.0, 0.0, 0.0);
            fail("Expected non-finite matrix to be rejected");
        } catch (IllegalArgumentException exception) {
            assertTrue(exception.getMessage().length()>0);
        }
        try {
            SvgTransform.parse("translate(1" + ",");
            fail("Expected malformed transform to be rejected");
        } catch (SvgParseException exception) {
            assertTrue(exception.getMessage().length()>0);
        }
    }
}
