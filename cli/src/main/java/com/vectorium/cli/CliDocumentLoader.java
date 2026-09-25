package com.vectorium.cli;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Locale;
import com.vectorium.core.model.VectorDocument;
import com.vectorium.core.svg.SvgParseException;
import com.vectorium.core.svg.SvgParser;
public final class CliDocumentLoader {
    private static final long MAX_SVG_FILE_BYTES=1024L*1024L*1024L;
    public VectorDocument load(File file) throws IOException, SvgParseException {
        if (file==null) {
            throw new IllegalArgumentException("file must not be null");
        }
        if (!file.isFile()||!file.canRead()) {
            throw new IOException("Input file is not readable: "+file.getPath());
        }
        String name=file.getName().toLowerCase(Locale.ROOT);
        if (name.endsWith(".svg")) {
            if (Files.size(file.toPath())>MAX_SVG_FILE_BYTES) {
                throw new IOException("Input file exceeds 1 GiB: "+file.getName());
            }
            byte[] bytes=Files.readAllBytes(file.toPath());
            String source=new String(bytes, StandardCharsets.UTF_8);
            return new SvgParser().parse(file.getName(), source);
        }
        if (name.endsWith(".png")||name.endsWith(".jpg")||name.endsWith(".jpeg")) {
            return new CliImageLoader().load(file);
        }
        throw new IOException("Unsupported input format: "+file.getName());
    }
}
