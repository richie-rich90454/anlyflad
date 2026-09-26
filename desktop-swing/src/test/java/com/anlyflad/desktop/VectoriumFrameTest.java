package com.anlyflad.desktop;
import java.awt.GraphicsEnvironment;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.fail;
import org.junit.jupiter.api.Assumptions;
public final class VectoriumFrameTest {
    @Test
    public void shouldCreateConfiguredDesktopFrame() throws Exception {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless());
        CreateFrame create=new CreateFrame();
        SwingUtilities.invokeAndWait(create);
        if (create.failure!=null) {
            fail("Frame creation failed", create.failure);
        }
        VectoriumFrame frame=create.frame;
        try {
            assertEquals("Anlyflad", frame.getTitle());
            assertEquals(1280, frame.getWidth());
            assertEquals(820, frame.getHeight());
            assertEquals(960, frame.getMinimumSize().width);
            assertEquals(640, frame.getMinimumSize().height);
            assertEquals(JFrame.DISPOSE_ON_CLOSE, frame.getDefaultCloseOperation());
            assertEquals("Ready", frame.getStatusBar().getStatusText());
            assertEquals(15, frame.getStageInspector().getStageCount());
            assertFalse(frame.getIconImages().isEmpty());
            assertSame(frame.getCanvas(), frame.getSplitPane().getLeftComponent());
            assertSame(frame.getStageInspector(), frame.getSplitPane().getRightComponent());
            assertSame(frame.getStatusBar(), ((javax.swing.JPanel)frame.getContentPane()).getComponent(2));
        } finally {
            SwingUtilities.invokeAndWait(new DisposeFrame(frame));
        }
    }
    private static final class CreateFrame implements Runnable {
        private VectoriumFrame frame;
        private Throwable failure;
        public void run() {
            try {
                frame=new VectoriumFrame();
            } catch (Throwable throwable) {
                failure=throwable;
            }
        }
    }
    private static final class DisposeFrame implements Runnable {
        private final VectoriumFrame frame;
        private DisposeFrame(VectoriumFrame frame) {
            this.frame=frame;
        }
        public void run() {
            frame.dispose();
        }
    }
}
