package com.vectorium.desktop;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Collections;
import java.util.Locale;
import javax.imageio.ImageIO;
import com.vectorium.core.model.RasterOrigin;
import com.vectorium.core.model.VectorDocument;
import com.vectorium.core.model.VectorPath;
import com.vectorium.core.svg.SvgParseException;
import com.vectorium.core.svg.SvgParser;
public final class DesktopDocumentLoader {
    private static final long MAX_FILE_BYTES=64L*1024L*1024L;
    private static final long MAX_PIXELS=64L*1024L*1024L;
    public VectorDocument load(File file) throws IOException, SvgParseException {
        if (file==null||!file.isFile()||!file.canRead()) {
            throw new IOException("Input file is not readable");
        }
        if (Files.size(file.toPath())>MAX_FILE_BYTES) {
            throw new IOException("Input file exceeds 64 MiB: "+file.getName());
        }
        String name=file.getName().toLowerCase(Locale.ROOT);
        if (name.endsWith(".svg")) {
            byte[] bytes=Files.readAllBytes(file.toPath());
            return new SvgParser().parse(file.getName(), new String(bytes, StandardCharsets.UTF_8));
        }
        if (!name.endsWith(".png")&&!name.endsWith(".jpg")&&!name.endsWith(".jpeg")) {
            throw new IOException("Unsupported input format: "+file.getName());
        }
        BufferedImage image=ImageIO.read(file);
        if (image==null||image.getWidth()<=0||image.getHeight()<=0) {
            throw new IOException("Unsupported or corrupt image: "+file.getName());
        }
        long length=(long)image.getWidth()*(long)image.getHeight();
        if (length>MAX_PIXELS||length>Integer.MAX_VALUE) {
            throw new IOException("Image dimensions exceed the 64-megapixel limit: "+file.getName());
        }
        int[] pixels=image.getRGB(0, 0, image.getWidth(), image.getHeight(), null, 0, image.getWidth());
        return new VectorDocument(file.getName(), new RasterOrigin(file.getName()), Collections.<VectorPath>emptyList(), image.getWidth(), image.getHeight(), pixels);
    }
}
