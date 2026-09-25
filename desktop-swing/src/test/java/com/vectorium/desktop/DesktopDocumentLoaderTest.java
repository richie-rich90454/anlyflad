package com.vectorium.desktop;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.CRC32;
import javax.imageio.ImageIO;
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
    public void shouldDecodePngAcrossTileBoundaries() throws Exception {
        int width=1027;
        int height=1027;
        BufferedImage image=new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        image.setRGB(0, 0, 0x10203040);
        image.setRGB(0, 1, 0x20304050);
        image.setRGB(1023, 0, 0x30405060);
        image.setRGB(1024, 0, 0x40506070);
        image.setRGB(0, 1024, 0x50607080);
        image.setRGB(width-1, height-1, 0x708090a0);
        File png=new File(temporaryDirectory.toFile(), "tiled.png");
        assertTrue(ImageIO.write(image, "png", png));
        VectorDocument document=new DesktopDocumentLoader().load(png);
        int[] pixels=document.getOwnedPixels();
        assertEquals(width, document.getWidth());
        assertEquals(height, document.getHeight());
        assertEquals(0x10203040, pixels[0]);
        assertEquals(0x20304050, pixels[width]);
        assertEquals(0x30405060, pixels[1023]);
        assertEquals(0x40506070, pixels[1024]);
        assertEquals(0x50607080, pixels[1024*width]);
        assertEquals(0x708090a0, pixels[pixels.length-1]);
    }
    @Test
    public void shouldRejectMismatchedPngHeaderBeforeDecode() throws Exception {
        File png=new File(temporaryDirectory.toFile(), "mismatched.png");
        assertTrue(ImageIO.write(new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB), "png", png));
        setPngWidth(png, 100L*1024L*1024L+1L);
        try {
            new DesktopDocumentLoader().load(png);
            throw new AssertionError("Expected mismatched PNG header dimensions to be rejected");
        } catch (IOException exception) {
            assertTrue(exception.getMessage().contains("100-megapixel"));
        }
    }
    @Test
    public void shouldRejectImageContentHeaderMismatch() throws Exception {
        File renamed=new File(temporaryDirectory.toFile(), "renamed.png");
        assertTrue(ImageIO.write(new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB), "jpeg", renamed));
        try {
            new DesktopDocumentLoader().load(renamed);
            throw new AssertionError("Expected image content header mismatch to be rejected");
        } catch (IOException exception) {
            assertTrue(exception.getMessage().contains("does not match"));
        }
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
    private static void setPngWidth(File png, long width) throws IOException {
        byte[] bytes=Files.readAllBytes(png.toPath());
        bytes[16]=(byte)(width>>>24);
        bytes[17]=(byte)(width>>>16);
        bytes[18]=(byte)(width>>>8);
        bytes[19]=(byte)width;
        CRC32 crc=new CRC32();
        crc.update(bytes, 12, 13);
        long checksum=crc.getValue();
        bytes[29]=(byte)(checksum>>>24);
        bytes[30]=(byte)(checksum>>>16);
        bytes[31]=(byte)(checksum>>>8);
        bytes[32]=(byte)checksum;
        Files.write(png.toPath(), bytes);
    }
}
