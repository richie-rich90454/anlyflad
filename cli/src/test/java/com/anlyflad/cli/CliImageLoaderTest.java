package com.anlyflad.cli;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.CRC32;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import com.anlyflad.core.model.VectorDocument;
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
        VectorDocument document=new CliImageLoader().load(png);
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
    public void shouldDecodeJpeg() throws Exception {
        BufferedImage image=new BufferedImage(1027, 3, BufferedImage.TYPE_INT_RGB);
        image.setRGB(0, 0, 0x102030);
        image.setRGB(1, 0, 0x506070);
        File jpeg=new File(temporaryDirectory.toFile(), "input.jpg");
        assertTrue(ImageIO.write(image, "jpeg", jpeg));
        VectorDocument document=new CliImageLoader().load(jpeg);
        assertEquals(1027, document.getWidth());
        assertEquals(3, document.getHeight());
    }
    @Test
    public void shouldRejectMismatchedPngHeaderBeforeDecode() throws Exception {
        File png=new File(temporaryDirectory.toFile(), "mismatched.png");
        assertTrue(ImageIO.write(new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB), "png", png));
        setPngWidth(png, 100L*1024L*1024L+1L);
        try {
            new CliImageLoader().load(png);
            fail("Expected mismatched PNG header dimensions to be rejected");
        } catch (IOException exception) {
            assertTrue(exception.getMessage().contains("100-megapixel"));
        }
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
            assertTrue(exception.getMessage().contains("does not match"));
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
