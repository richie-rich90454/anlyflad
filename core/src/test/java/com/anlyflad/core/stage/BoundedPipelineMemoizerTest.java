package com.anlyflad.core.stage;
import com.anlyflad.core.model.VectorDocument;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
public final class BoundedPipelineMemoizerTest {
    @Test
    public void shouldMatchInputIdentityAndConfigurationHash() {
        BoundedPipelineMemoizer memoizer=new BoundedPipelineMemoizer();
        VectorDocument input=VectorDocument.emptySvg("input", 1, 1);
        VectorDocument equalButDifferent=VectorDocument.emptySvg("input", 1, 1);
        VectorDocument output=VectorDocument.emptySvg("output", 2, 2);
        memoizer.put(input, 7L, output);
        assertSame(output, memoizer.get(input, 7L));
        assertNull(memoizer.get(equalButDifferent, 7L));
        assertNull(memoizer.get(input, 8L));
    }
    @Test
    public void shouldBoundReplaceAndClearEntries() {
        BoundedPipelineMemoizer memoizer=new BoundedPipelineMemoizer();
        for (int index=0;index<20;index++) {
            VectorDocument input=VectorDocument.emptySvg("input"+index, 1, 1);
            memoizer.put(input, index, input);
        }
        assertEquals(16, memoizer.size());
        VectorDocument input=VectorDocument.emptySvg("replacement", 1, 1);
        VectorDocument output=VectorDocument.emptySvg("output", 2, 2);
        memoizer.put(input, 1L, output);
        assertEquals(16, memoizer.size());
        assertSame(output, memoizer.get(input, 1L));
        memoizer.clear();
        assertEquals(0, memoizer.size());
        assertNull(memoizer.get(input, 1L));
    }
    @Test
    public void shouldRejectNullEntries() {
        BoundedPipelineMemoizer memoizer=new BoundedPipelineMemoizer();
        try {
            memoizer.put(null, 1L, VectorDocument.emptySvg("output", 1, 1));
            fail("Expected null input to be rejected");
        } catch (IllegalArgumentException exception) {
            assertTrue(exception.getMessage().length()>0);
        }
        try {
            memoizer.put(VectorDocument.emptySvg("input", 1, 1), 1L, null);
            fail("Expected null output to be rejected");
        } catch (IllegalArgumentException exception) {
            assertTrue(exception.getMessage().length()>0);
        }
    }
}
