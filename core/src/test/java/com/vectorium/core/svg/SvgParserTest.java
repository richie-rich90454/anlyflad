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
}
