package com.anlyflad.core.docs;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
public final class DocumentationResourcesTest {
    private static final String[] PATHS={"/docs/README.md", "/docs/architecture.md", "/docs/performance.md", "/docs/teavm.md"};
    @Test
    public void shouldBundleEveryDocumentationPage() throws Exception {
        for (int index=0;index<PATHS.length;index++) {
            String markdown=read(PATHS[index]);
            assertNotNull(markdown,PATHS[index]+" must be bundled");
            String html=MarkdownRenderer.render(markdown);
            assertFalse(html.trim().isEmpty(),PATHS[index]+" must render");
            assertTrue(html.contains("<h1>")||html.contains("<h2>"),PATHS[index]+" must contain a heading");
        }
    }
    private static String read(String path) throws IOException {
        InputStream stream=DocumentationResourcesTest.class.getResourceAsStream(path);
        if (stream==null) {
            return null;
        }
        try {
            ByteArrayOutputStream output=new ByteArrayOutputStream();
            byte[] buffer=new byte[8192];
            int count;
            while ((count=stream.read(buffer))>=0) {
                output.write(buffer,0,count);
            }
            return new String(output.toByteArray(), StandardCharsets.UTF_8);
        } finally {
            stream.close();
        }
    }
}
