package com.vectorium.cli;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
public final class AnlyfladCliTest {
    @TempDir
    public Path temporaryDirectory;
    @Test
    public void shouldConvertSvgWithOverridesAndReportStages() throws Exception {
        File input=new File(temporaryDirectory.toFile(), "input.svg");
        File output=new File(temporaryDirectory.toFile(), "output.svg");
        Files.write(input.toPath(), "<svg width='20' height='10'><path d='M0 0 L10 0 L10 10 Z'/></svg>".getBytes(StandardCharsets.UTF_8));
        ByteArrayOutputStream outputBytes=new ByteArrayOutputStream();
        ByteArrayOutputStream errorBytes=new ByteArrayOutputStream();
        int exitCode=new AnlyfladCli(new PrintStream(outputBytes), new PrintStream(errorBytes)).execute(input.getAbsolutePath(), "--output", output.getAbsolutePath(), "--mode", "binary", "--preset", "fast", "--stage", "smooth.passes=2", "--no-stage", "layer-sort");
        String stdout=outputBytes.toString("UTF-8");
        assertEquals(0, exitCode);
        assertTrue(errorBytes.toString("UTF-8").isEmpty());
        assertTrue(output.isFile());
        assertTrue(new String(Files.readAllBytes(output.toPath()), StandardCharsets.UTF_8).startsWith("<svg"));
        assertTrue(stdout.contains("smooth\tAPPLIED"));
        assertTrue(stdout.contains("layer-sort\tSKIPPED"));
        assertTrue(stdout.contains("Result:"));
    }
    @Test
    public void shouldConvertPngRaster() throws Exception {
        BufferedImage image=new BufferedImage(4, 4, BufferedImage.TYPE_INT_ARGB);
        for (int y=0;y<4;y++) {
            for (int x=0;x<4;x++) {
                image.setRGB(x, y, x<2?0xFFFF0000:0xFF0000FF);
            }
        }
        File input=new File(temporaryDirectory.toFile(), "input.png");
        File output=new File(temporaryDirectory.toFile(), "output.svg");
        assertTrue(ImageIO.write(image, "png", input));
        ByteArrayOutputStream outputBytes=new ByteArrayOutputStream();
        ByteArrayOutputStream errorBytes=new ByteArrayOutputStream();
        int exitCode=new AnlyfladCli(new PrintStream(outputBytes), new PrintStream(errorBytes)).execute(input.getAbsolutePath(), "-o", output.getAbsolutePath());
        assertEquals(0, exitCode);
        assertTrue(output.isFile());
        String svg=new String(Files.readAllBytes(output.toPath()), StandardCharsets.UTF_8);
        assertTrue(svg.startsWith("<svg"));
        assertTrue(svg.contains("#ff0000"));
        assertTrue(svg.contains("#0000ff"));
    }
    @Test
    public void shouldExposeVersionNoCleanAndRejectInvalidConfiguration() throws Exception {
        File input=new File(temporaryDirectory.toFile(), "input.svg");
        File output=new File(temporaryDirectory.toFile(), "output.svg");
        Files.write(input.toPath(), "<svg width='10' height='10'><rect width='5' height='5'/></svg>".getBytes(StandardCharsets.UTF_8));
        ByteArrayOutputStream outputBytes=new ByteArrayOutputStream();
        ByteArrayOutputStream errorBytes=new ByteArrayOutputStream();
        AnlyfladCli command=new AnlyfladCli(new PrintStream(outputBytes), new PrintStream(errorBytes));
        assertEquals(0, command.execute("--version"));
        assertTrue(outputBytes.toString("UTF-8").contains("1.0.0"));
        assertEquals(0, command.execute(input.getAbsolutePath(), "-o", output.getAbsolutePath(), "--no-clean"));
        assertTrue(outputBytes.toString("UTF-8").contains("layer-sort\tSKIPPED"));
        errorBytes.reset();
        assertEquals(1, command.execute(input.getAbsolutePath(), "-o", output.getAbsolutePath(), "--preset", "invalid"));
        assertTrue(errorBytes.toString("UTF-8").contains("Unknown preset"));
        errorBytes.reset();
        assertEquals(1, command.execute(input.getAbsolutePath(), "-o", output.getAbsolutePath(), "--no-stage", "serialize"));
        assertTrue(errorBytes.toString("UTF-8").contains("cannot be disabled"));
        errorBytes.reset();
        assertEquals(1, command.execute(input.getAbsolutePath(), "-o", output.getAbsolutePath(), "--no-stage", "vectorize"));
        assertTrue(errorBytes.toString("UTF-8").contains("cannot be disabled"));
        errorBytes.reset();
        assertEquals(1, command.execute(input.getAbsolutePath(), "-o", output.getAbsolutePath(), "--mode", "invalid"));
        assertTrue(errorBytes.toString("UTF-8").contains("raster mode"));
    }
    @Test
    public void shouldReturnUserErrorsAndHelp() throws Exception {
        ByteArrayOutputStream outputBytes=new ByteArrayOutputStream();
        ByteArrayOutputStream errorBytes=new ByteArrayOutputStream();
        AnlyfladCli command=new AnlyfladCli(new PrintStream(outputBytes), new PrintStream(errorBytes));
        assertEquals(1, command.execute("missing.svg", "-o", "output.svg"));
        assertTrue(errorBytes.toString("UTF-8").contains("Error:"));
        errorBytes.reset();
        assertEquals(1, command.execute("input.svg"));
        assertTrue(errorBytes.toString("UTF-8").contains("Missing required option"));
        errorBytes.reset();
        assertEquals(0, command.execute("--help"));
        assertTrue(outputBytes.toString("UTF-8").contains("Usage:"));
    }
    @Test
    public void shouldConvertSvgWithUtf8ByteOrderMark() throws Exception {
        File input=new File(temporaryDirectory.toFile(), "bom.svg");
        File output=new File(temporaryDirectory.toFile(), "bom-output.svg");
        byte[] content="<svg width='10' height='10'><rect width='5' height='5'/></svg>".getBytes(StandardCharsets.UTF_8);
        byte[] bytes=new byte[content.length+3];
        bytes[0]=(byte)0xEF;
        bytes[1]=(byte)0xBB;
        bytes[2]=(byte)0xBF;
        System.arraycopy(content, 0, bytes, 3, content.length);
        Files.write(input.toPath(), bytes);
        int exitCode=new AnlyfladCli(new PrintStream(new ByteArrayOutputStream()), new PrintStream(new ByteArrayOutputStream())).execute(input.getAbsolutePath(), "-o", output.getAbsolutePath());
        assertEquals(0, exitCode);
        assertTrue(output.isFile());
    }
    @Test
    public void shouldRejectUnsupportedInputWithoutWritingOutput() throws Exception {
        File input=new File(temporaryDirectory.toFile(), "input.txt");
        File output=new File(temporaryDirectory.toFile(), "output.svg");
        Files.write(input.toPath(), "not an image".getBytes(StandardCharsets.UTF_8));
        ByteArrayOutputStream errorBytes=new ByteArrayOutputStream();
        int exitCode=new AnlyfladCli(new PrintStream(new ByteArrayOutputStream()), new PrintStream(errorBytes)).execute(input.getAbsolutePath(), "-o", output.getAbsolutePath());
        assertEquals(1, exitCode);
        assertFalse(output.exists());
        assertTrue(errorBytes.toString("UTF-8").contains("Unsupported input format"));
    }
}
