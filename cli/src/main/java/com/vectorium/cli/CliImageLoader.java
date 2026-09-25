package com.vectorium.cli;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Collections;
import java.util.Iterator;
import java.util.Locale;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import com.vectorium.core.model.RasterOrigin;
import com.vectorium.core.model.VectorDocument;
import com.vectorium.core.model.VectorPath;
public final class CliImageLoader {
    private static final long MAX_FILE_BYTES=64L*1024L*1024L;
    private static final long MAX_PIXELS=64L*1024L*1024L;
    public VectorDocument load(File file) throws IOException {
        if (file==null) {
            throw new IllegalArgumentException("file must not be null");
        }
        if (!file.isFile()||!file.canRead()) {
            throw new IOException("Input file is not readable: "+file.getPath());
        }
        if (Files.size(file.toPath())>MAX_FILE_BYTES) {
            throw new IOException("Image file exceeds 64 MiB: "+file.getName());
        }
        String name=file.getName().toLowerCase(Locale.ROOT);
        boolean pngName=name.endsWith(".png");
        boolean jpegName=name.endsWith(".jpg")||name.endsWith(".jpeg");
        if (!pngName&&!jpegName) {
            throw new IOException("Unsupported input format: "+file.getName());
        }
        ImageInputStream imageInput=ImageIO.createImageInputStream(file);
        if (imageInput==null) {
            throw new IOException("Unable to open image: "+file.getName());
        }
        try {
            Iterator<ImageReader> readers=ImageIO.getImageReaders(imageInput);
            if (!readers.hasNext()) {
                throw new IOException("Unsupported or corrupt image: "+file.getName());
            }
            ImageReader reader=readers.next();
            try {
                return readImage(file, imageInput, reader, pngName, jpegName);
            } finally {
                reader.dispose();
            }
        } finally {
            imageInput.close();
        }
    }
    private static VectorDocument readImage(File file, ImageInputStream imageInput, ImageReader reader, boolean pngName, boolean jpegName) throws IOException {
        reader.setInput(imageInput, true, true);
        String format=reader.getFormatName().toLowerCase(Locale.ROOT);
        boolean pngContent="png".equals(format);
        boolean jpegContent="jpg".equals(format)||"jpeg".equals(format);
        if ((pngName&&!pngContent)||(jpegName&&!jpegContent)) {
            throw new IOException("Image content does not match the .png or .jpeg extension: "+file.getName());
        }
        int width=reader.getWidth(0);
        int height=reader.getHeight(0);
        if (width<=0||height<=0) {
            throw new IOException("Image dimensions must be positive: "+file.getName());
        }
        long length=(long)width*(long)height;
        if (length>MAX_PIXELS||length>Integer.MAX_VALUE) {
            throw new IOException("Image dimensions exceed the 64-megapixel limit: "+file.getName());
        }
        BufferedImage image=reader.read(0);
        if (image==null) {
            throw new IOException("Unsupported or corrupt image: "+file.getName());
        }
        if (image.getWidth()!=width||image.getHeight()!=height) {
            throw new IOException("Decoded image dimensions do not match the header: "+file.getName());
        }
        int[] pixels=image.getRGB(0, 0, width, height, null, 0, width);
        return new VectorDocument(file.getName(), new RasterOrigin(file.getName()), Collections.<VectorPath>emptyList(), width, height, pixels);
    }
}
