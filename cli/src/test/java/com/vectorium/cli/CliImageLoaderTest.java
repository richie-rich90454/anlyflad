package com.vectorium.cli;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import com.vectorium.core.model.VectorDocument;
public final class CliImageLoaderTest {
    @TempDir
    public Path temporaryDirectory;
    @Test
    public void shouldDecodePngIntoOwnedRasterDocument() throws Exception {
        BufferedImage image=new BufferedImage(2, 1, BufferedImage.TYPE_INT_ARGB);
        image.setRGB(0, 0, 0x10203040);
        image.setRGB(1, 0, 0x50607080);
        File png=new File(temporaryDirectory.toFile(), "input.png");
        assertTrue(ImageIO.write(image, "png", png));
        VectorDocument document=new CliImageLoader().load(png);
        assertEquals("input.png", document.getDocumentId());
        assertTrue(document.getOrigin().isRaster());
        assertEquals(2, document.getWidth());
        assertEquals(1, document.getHeight());
        assertEquals(0x10203040, document.getOwnedPixels()[0]);
        assertEquals(0x50607080, document.getOwnedPixels()[1]);
    }
    @Test
    public void shouldDecodeJpeg() throws Exception {
        BufferedImage image=new BufferedImage(2, 1, BufferedImage.TYPE_INT_RGB);
        image.setRGB(0, 0, 0x102030);
        image.setRGB(1, 0, 0x506070);
        File jpeg=new File(temporaryDirectory.toFile(), "input.jpg");
        assertTrue(ImageIO.write(image, "jpeg", jpeg));
        VectorDocument document=new CliImageLoader().load(jpeg);
        assertEquals(2, document.getWidth());
        assertEquals(1, document.getHeight());
    }
    @Test
    public void shouldRejectMissingRenamedAndUnsupportedImages() throws Exception {
        try {
            new CliImageLoader().load(new File(temporaryDirectory.toFile(), "missing.png"));
            fail("Expected missing image to be rejected");
        } catch (IOException exception) {
            assertTrue(exception.getMessage().length()>0);
        }
        File renamed=new File(temporaryDirectory.toFile(), "renamed.png");
        assertTrue(ImageIO.write(new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB), "jpeg", renamed));
        try {
            new CliImageLoader().load(renamed);
            fail("Expected renamed unsupported image to be rejected");
        } catch (IOException exception) {
            assertTrue(exception.getMessage().length()>0);
        }
        File unsupported=new File(temporaryDirectory.toFile(), "input.txt");
        assertTrue(ImageIO.write(new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB), "png", unsupported));
        try {
            new CliImageLoader().load(unsupported);
            fail("Expected unsupported image to be rejected");
        } catch (IOException exception) {
            assertTrue(exception.getMessage().length()>0);
        }
    }
}
