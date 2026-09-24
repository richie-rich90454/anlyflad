package com.vectorium.core.perf;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
public final class SquaredDeltaTableTest {
    @Test
    public void shouldMatchEverySquaredChannelDelta() {
        for (int first=0;first<256;first++) {
            for (int second=0;second<256;second++) {
                int delta=first-second;
                assertEquals(delta*delta, SquaredDeltaTable.get(first, second));
            }
        }
    }
    @Test
    public void shouldRejectChannelsOutsideByteRange() {
        int[][] invalid={new int[]{-1, 0}, new int[]{0, 256}, new int[]{256, -1}};
        for (int index=0;index<invalid.length;index++) {
            try {
                SquaredDeltaTable.get(invalid[index][0], invalid[index][1]);
                fail("Expected invalid channel to be rejected");
            } catch (IllegalArgumentException exception) {
                assertTrue(exception.getMessage().length()>0);
            }
        }
    }
}
