package com.vectorium.core.svg;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import javax.xml.parsers.DocumentBuilderFactory;
import org.xml.sax.InputSource;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import com.vectorium.core.model.Color;
import com.vectorium.core.model.PathId;
import com.vectorium.core.model.RasterOrigin;
import com.vectorium.core.model.SvgOrigin;
import com.vectorium.core.model.VectorDocument;
import com.vectorium.core.model.VectorPath;
public final class SvgWriterTest {
    @Test
    public void shouldWriteDeterministicNumericSvg() throws Exception {
        VectorPath path=new VectorPath(PathId.zero(), new double[]{0.0, 0.0, 2.0, 0.0, 2.0, 2.0}, true, new Color(255, 0, 0, 255), 0.5);
        List<VectorPath> paths=new ArrayList<VectorPath>();
        paths.add(path);
        VectorDocument document=new VectorDocument("document", new SvgOrigin("document"), paths, 4, 3, new int[12]);
        SvgWriter writer=new SvgWriter();
        String first=writer.write(document);
        String second=writer.write(document);
        assertEquals(first, second);
        assertTrue(first.contains("width=\"4\""));
        assertTrue(first.contains("height=\"3\""));
        assertTrue(first.contains("viewBox=\"0 0 4 3\""));
        assertTrue(first.contains("fill=\"#ff0000\""));
        assertTrue(first.contains("fill-opacity=\"0.5\""));
        assertTrue(first.indexOf("Z")>0);
    }
    @Test
    public void shouldWriteToWriterAndRoundTrip() throws Exception {
        VectorPath path=new VectorPath(PathId.zero(), new double[]{1.0, 2.0, 3.0, 4.0}, false, new Color(0, 128, 255, 255), 1.0);
        List<VectorPath> paths=new ArrayList<VectorPath>();
        paths.add(path);
        VectorDocument document=new VectorDocument("roundtrip", new SvgOrigin("roundtrip"), paths, 5, 6, new int[30]);
        StringWriter output=new StringWriter();
        new SvgWriter().write(document, output);
        VectorDocument parsed=new SvgParser().parse("roundtrip", output.toString());
        assertEquals(1, parsed.getPaths().size());
        assertEquals(1.0, parsed.getPaths().get(0).getCoordinates()[0], 0.0);
        assertEquals(2.0, parsed.getPaths().get(0).getCoordinates()[1], 0.0);
        assertEquals(3.0, parsed.getPaths().get(0).getCoordinates()[2], 0.0);
        assertEquals(4.0, parsed.getPaths().get(0).getCoordinates()[3], 0.0);
    }
    @Test
    public void shouldPreserveRasterColorsAndRequestCrispEdges() throws Exception {
        VectorPath path=new VectorPath(PathId.zero(), new double[]{0.0, 0.0, 1.0, 0.0, 1.0, 1.0, 0.0, 1.0}, true, new Color(10, 20, 30, 128), 1.0);
        VectorDocument document=new VectorDocument("raster.png", new RasterOrigin("raster.png"), java.util.Collections.singletonList(path), 1, 1, new int[]{0x800A141E});
        String output=SvgWriter.write(document);
        assertTrue(output.contains("shape-rendering=\"crispEdges\""));
        assertTrue(output.contains("fill=\"#0a141e\""));
        assertTrue(output.contains("fill-opacity=\"0.5019607843137255\""));
        assertWellFormed(output);
        assertEquals(1, new SvgParser().parse("output.svg", output).getPaths().size());
    }
    @Test
    public void shouldPreserveParsedSvgSourceForColorPassthrough() throws Exception {
        String source="<svg width='20' height='10'><title>é</title><rect width='10' height='5' fill='red' stroke='blue'/><text x='1' y='8'>label</text></svg>";
        VectorDocument document=new SvgParser().parse("source.svg", source);
        assertEquals(source, SvgWriter.write(document));
        assertEquals(source.getBytes(StandardCharsets.UTF_8).length, SvgWriter.utf8Length(source));
    }
    private static void assertWellFormed(String svg) throws Exception {
        DocumentBuilderFactory factory=DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        factory.newDocumentBuilder().parse(new InputSource(new StringReader(svg)));
    }
    @Test
    public void shouldRejectNullDocument() {
        try {
            new SvgWriter().write(null);
            fail("Expected null document to be rejected");
        } catch (IllegalArgumentException exception) {
            assertTrue(exception.getMessage().length()>0);
        }
    }
}
