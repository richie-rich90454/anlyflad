package com.anlyflad.core.docs;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
public final class MarkdownRendererTest {
    @Test
    public void shouldRenderHeadingsParagraphsListsAndRules() {
        String html=MarkdownRenderer.render("# Title\n\nText with **bold** and `code`.\n\n- one\n- two\n\n---\n");
        assertTrue(html.contains("<h1>Title</h1>"));
        assertTrue(html.contains("<strong>bold</strong>"));
        assertTrue(html.contains("<code>code</code>"));
        assertTrue(html.contains("<ul>"));
        assertTrue(html.contains("<li>one</li>"));
        assertTrue(html.contains("<hr>"));
    }
    @Test
    public void shouldRenderTablesAndCodeFences() {
        String html=MarkdownRenderer.render("| A | B |\n|---|---|\n| 1 | 2 |\n\n```text\n<tag>\n```\n");
        assertTrue(html.contains("<table>"));
        assertTrue(html.contains("<th>A</th>"));
        assertTrue(html.contains("<td>1</td>"));
        assertTrue(html.contains("<pre><code>"));
        assertTrue(html.contains("&lt;tag&gt;"));
    }
    @Test
    public void shouldEscapeAndSanitizeLinks() {
        String html=MarkdownRenderer.render("<script>alert(1)</script> [x](javascript:alert(1)) [y](architecture.md)\n");
        assertFalse(html.contains("<script>"));
        assertTrue(html.contains("&lt;script&gt;"));
        assertTrue(html.contains("href=\"#\""));
        assertTrue(html.contains("href=\"architecture.md\""));
    }
}
