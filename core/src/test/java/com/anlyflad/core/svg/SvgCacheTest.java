package com.anlyflad.core.svg;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import com.anlyflad.core.model.VectorDocument;
public final class SvgCacheTest {
    @Test
    public void shouldCacheSerializedSvgByDocumentIdentity() {
        SvgCache cache=new SvgCache();
        VectorDocument document=VectorDocument.emptySvg("one", 1, 1);
        VectorDocument equalButDifferent=VectorDocument.emptySvg("one", 2, 2);
        String first=cache.get(document);
        assertSame(first, cache.get(document));
        String replacement=cache.get(equalButDifferent);
        assertNotSame(first, replacement);
        assertSame(replacement, cache.get(equalButDifferent));
        assertTrue(!cache.invalidate(document));
        assertSame(replacement, cache.get(equalButDifferent));
        assertTrue(cache.invalidate(equalButDifferent));
        assertEquals(0, cache.size());
    }
    @Test
    public void shouldBoundEntriesAndReplaceExistingDocumentIds() {
        SvgCache cache=new SvgCache();
        VectorDocument first=VectorDocument.emptySvg("first", 1, 1);
        cache.put(first, SvgWriter.write(first));
        VectorDocument replacement=VectorDocument.emptySvg("first", 2, 2);
        cache.put(replacement, SvgWriter.write(replacement));
        assertEquals(1, cache.size());
        assertSame(cache.get(replacement), cache.get(replacement));
        for (int index=0;index<40;index++) {
            VectorDocument document=VectorDocument.emptySvg("key"+index, 1, 1);
            cache.get(document);
        }
        assertEquals(32, cache.size());
        assertEquals(32, cache.capacity());
    }
    @Test
    public void shouldInvalidateByIdAndClear() {
        SvgCache cache=new SvgCache();
        VectorDocument one=VectorDocument.emptySvg("one", 1, 1);
        VectorDocument two=VectorDocument.emptySvg("two", 1, 1);
        cache.get(one);
        cache.get(two);
        assertTrue(cache.invalidate("one"));
        assertTrue(!cache.invalidate("one"));
        assertEquals(1, cache.size());
        cache.clear();
        assertEquals(0, cache.size());
    }
    @Test
    public void shouldRejectInvalidCacheEntries() {
        try {
            new SvgCache().get(null);
            fail("Expected null document to be rejected");
        } catch (IllegalArgumentException exception) {
            assertTrue(exception.getMessage().length()>0);
        }
        try {
            new SvgCache().put(VectorDocument.emptySvg("one", 1, 1), null);
            fail("Expected null SVG to be rejected");
        } catch (IllegalArgumentException exception) {
            assertTrue(exception.getMessage().length()>0);
        }
        try {
            new SvgCache().invalidate(" ");
            fail("Expected blank id to be rejected");
        } catch (IllegalArgumentException exception) {
            assertTrue(exception.getMessage().length()>0);
        }
    }
}
