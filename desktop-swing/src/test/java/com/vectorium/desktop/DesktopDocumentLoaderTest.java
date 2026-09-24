package com.vectorium.desktop;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import com.vectorium.core.model.VectorDocument;
public final class DesktopDocumentLoaderTest {
    @TempDir
    public Path temporaryDirectory;
    @Test
    public void shouldLoadSvg() throws Exception {
        File file=new File(temporaryDirectory.toFile(), "input.svg");
        Files.write(file.toPath(), "<svg width='12' height='8'><rect width='5' height='4'/></svg>".getBytes(StandardCharsets.UTF_8));
        VectorDocument document=new DesktopDocumentLoader().load(file);
        assertEquals("input.svg", document.getDocumentId());
        assertEquals(12, document.getWidth());
        assertEquals(1, document.getPaths().size());
    }
    @Test
    public void shouldRejectUnsupportedInput() throws Exception {
        File file=new File(temporaryDirectory.toFile(), "input.txt");
        Files.write(file.toPath(), "text".getBytes(StandardCharsets.UTF_8));
        try {
            new DesktopDocumentLoader().load(file);
        } catch (java.io.IOException exception) {
            assertTrue(exception.getMessage().contains("Unsupported"));
            return;
        }
        throw new AssertionError("Expected unsupported input to fail");
    }
}
