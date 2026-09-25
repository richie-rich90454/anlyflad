package com.vectorium.core.svg;
import java.io.StringReader;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import com.vectorium.core.model.VectorDocument;
import com.vectorium.core.model.VectorPath;
public final class SvgParserTest {
    @Test
    public void shouldParseShapesAndMeaningfulDimensions() throws Exception {
        VectorDocument document=new SvgParser().parse("drawing.svg", "<svg width='20' height='10'><rect x='1' y='2' width='3' height='4'/><circle cx='5' cy='5' r='2'/><ellipse cx='8' cy='5' rx='2' ry='1'/><line x1='0' y1='0' x2='4' y2='4'/><polygon points='0,0 4,0 4,4'/><polyline points='0,0 2,2'/></svg>");
        assertEquals("drawing.svg", document.getDocumentId());
        assertEquals(20, document.getWidth());
        assertEquals(10, document.getHeight());
        assertEquals(6, document.getPaths().size());
        assertTrue(document.getPaths().get(0).isClosed());
        assertTrue(!document.getPaths().get(3).isClosed());
    }
    @Test
    public void shouldPreserveSafeXmlDeclaration() throws Exception {
        String source="<?xml version='1.0' encoding='UTF-8'?><svg width='10' height='10'><path d='M0 0 L1 1'/></svg>";
        VectorDocument document=new SvgParser().parse("declaration.svg", source);
        assertEquals(source, document.getSourceSvg());
    }
    @Test
    public void shouldInheritGroupStyleAndTransform() throws Exception {
        VectorDocument document=new SvgParser().parse("group.svg", "<svg width='20' height='20' fill='red' opacity='0.5' transform='translate(1 2)'><g fill='#00ff00' opacity='0.5' transform='scale(2)'><path d='M0 0 L1 0'/></g></svg>");
        VectorPath path=document.getPaths().get(0);
        assertEquals(0, path.getId().getValue());
        assertEquals(255, path.getFill().getGreen());
        assertEquals(0, path.getFill().getRed());
        assertEquals(0.25, path.getOpacity(), 1.0e-9);
        assertEquals(1.0, path.getCoordinates()[0], 0.0);
        assertEquals(2.0, path.getCoordinates()[1], 0.0);
        assertEquals(3.0, path.getCoordinates()[2], 0.0);
        assertEquals(2.0, path.getCoordinates()[3], 0.0);
    }
    @Test
    public void shouldParseStyleEntitiesAndSkipDefs() throws Exception {
        VectorDocument document=new SvgParser().parse("entities.svg", "<svg width='10' height='10'><!-- ignored --><defs><path d='M99 99 L100 100'/><metadata>text &amp; more</metadata></defs><path fill='&#114;ed' style='fill:#0000ff; fill-opacity:0.25' d='M0 0 L1 1'/></svg>");
        assertEquals(1, document.getPaths().size());
        VectorPath path=document.getPaths().get(0);
        assertEquals(255, path.getFill().getBlue());
        assertEquals(0, path.getFill().getRed());
        assertEquals(0.25, path.getOpacity(), 0.0);
    }
    @Test
    public void shouldReadReaderAndUseViewBoxWhenDimensionsAreAbsent() throws Exception {
        VectorDocument document=new SvgParser().parse(new StringReader("<svg viewBox='0 0 30 40'><path d='M0 0 L2 0'/></svg>"), "reader.svg");
        assertEquals(30, document.getWidth());
        assertEquals(40, document.getHeight());
    }
    @Test
    public void shouldRejectInvalidRootsDimensionsAndValues() {
        String[] invalid=new String[]{"<html></html>", "<svg width='-1' height='2'/>", "<svg width='NaN' height='2'/>", "<svg><path d='M0 0 LNaN 1'/></svg>", "<svg><script>alert(1)</script></svg>", "<svg><path href='https://example.com/a' d='M0 0 L1 1'/></svg>", "<svg><path ping='https://example.com/a' d='M0 0 L1 1'/></svg>", "<svg><path onload='alert(1)' d='M0 0 L1 1'/></svg>", "<svg><animateColor attributeName='fill' values='red;blue'/></svg>", "<!DOCTYPE svg><svg/>", "<?xml-stylesheet href='https://example.com/a'?><svg/>", "<svg><style>*{fill:url(https://example.com/a)}</style></svg>"};
        for (int index=0;index<invalid.length;index++) {
            try {
                new SvgParser().parse("bad.svg", invalid[index]);
                fail("Expected malformed SVG to be rejected");
            } catch (SvgParseException exception) {
                assertTrue(exception.getMessage().length()>0);
            }
        }
    }
    @Test
    public void shouldPreserveFillRuleAndCompoundSubpaths() throws Exception {
        String source="<svg width='20' height='20'><path fill-rule='evenodd' d='M0 0 L10 0 L10 10 Z M2 2 L8 2 L8 8 Z'/><path fill-rule='nonzero' style='fill-rule: evenodd' d='M0 0 L1 0 L1 1 Z'/></svg>";
        VectorDocument document=new SvgParser().parse("compound.svg", source);
        assertEquals(2, document.getPaths().size());
        VectorPath compound=document.getPaths().get(0);
        assertTrue(compound.isCompound());
        assertEquals(2, compound.getRingCount());
        assertEquals(VectorPath.FillRule.EVEN_ODD, compound.getFillRule());
        assertEquals(0.0, compound.getRing(0)[0], 0.0);
        assertEquals(2.0, compound.getRing(1)[0], 0.0);
        VectorPath styled=document.getPaths().get(1);
        assertEquals(VectorPath.FillRule.EVEN_ODD, styled.getFillRule());
    }
    @Test
    public void shouldApplyTransformsToCubicControlsAndKeepSvgClosuresContinuous() throws Exception {
        VectorDocument document=new SvgParser().parse("curve.svg", "<svg width='20' height='20'><path transform='translate(10 20)' d='M0 0 C1 2 3 4 5 6 Z'/></svg>");
        VectorPath path=document.getPaths().get(0);
        assertTrue(path.hasCubicData());
        assertEquals(2, path.getCubicSegmentCount());
        double[] cubic=path.getCubicRing(0);
        assertEquals(11.0, cubic[2], 0.0);
        assertEquals(22.0, cubic[3], 0.0);
        assertEquals(13.0, cubic[4], 0.0);
        assertEquals(24.0, cubic[5], 0.0);
        assertEquals(15.0, cubic[6], 0.0);
        assertEquals(26.0, cubic[7], 0.0);
        double[] fallback=path.getCoordinates();
        assertEquals(cubic[0], fallback[0], 0.0);
        assertEquals(cubic[1], fallback[1], 0.0);
        assertEquals(cubic[cubic.length-2], fallback[fallback.length-2], 0.0);
        assertEquals(cubic[cubic.length-1], fallback[fallback.length-1], 0.0);
    }
    @Test
    public void shouldRejectMalformedCubicGeometry() {
        String[] invalid=new String[]{"<svg><path d='M0 0 C1 1 2 2 3'/></svg>", "<svg><path fill-rule='invalid' d='M0 0 L1 0 L1 1 Z'/></svg>"};
        for (int index=0;index<invalid.length;index++) {
            try {
                new SvgParser().parse("bad.svg", invalid[index]);
                fail("Expected malformed SVG geometry to be rejected");
            } catch (SvgParseException exception) {
                assertTrue(exception.getMessage().length()>0);
            }
        }
    }
}
