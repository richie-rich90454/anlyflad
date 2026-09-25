package com.vectorium.web;
import com.vectorium.core.model.RasterOrigin;
import com.vectorium.core.model.VectorDocument;
import com.vectorium.core.model.VectorPath;
import com.vectorium.core.stage.BoundedPipelineMemoizer;
import com.vectorium.core.stage.Pipeline;
import com.vectorium.core.stage.PipelineConfig;
import com.vectorium.core.stage.PipelineLogger;
import com.vectorium.core.stage.RasterMode;
import com.vectorium.core.stage.StageException;
import com.vectorium.core.stage.StageRegistry;
import com.vectorium.core.svg.SvgCache;
import com.vectorium.core.svg.SvgParseException;
import com.vectorium.core.svg.SvgParser;
import java.util.Collections;
public final class VectoriumWeb {
    private static final long MAX_FILE_BYTES=16L*1024L*1024L;
    private static final long MAX_PIXELS=16L*1024L*1024L;
    private WebDom.Document document;
    private WebDom.Element fileInput;
    private WebDom.Element modeInput;
    private WebDom.Element runButton;
    private WebDom.Element preview;
    private WebDom.Element download;
    private WebDom.Element status;
    private String previewUrl;
    private long loadSequence;
    private VectorDocument current;
    public static void main(String[] args) {
        new VectoriumWeb().start();
    }
    public void start() {
        document=WebDom.document();
        fileInput=require("file-input");
        modeInput=require("mode-input");
        runButton=require("run-button");
        preview=require("preview");
        download=require("download");
        status=require("status");
        setStatus("Choose a PNG, JPEG, or SVG file.");
        fileInput.addEventListener("change", new WebDom.EventListener() {
            public void handleEvent(WebDom.Event event) {
                readFile();
            }
        });
        runButton.addEventListener("click", new WebDom.EventListener() {
            public void handleEvent(WebDom.Event event) {
                vectorize();
            }
        });
    }
    private void readFile() {
        final long requestId=++loadSequence;
        current=null;
        clearPreview();
        WebDom.FileList files=fileInput.getFiles();
        if (files==null||files.getLength()==0) {
            setStatus("Choose a file first.");
            return;
        }
        WebDom.File file=files.get(0);
        double fileSize=file.getSize();
        if (!Double.isFinite(fileSize)||fileSize<0.0||fileSize>MAX_FILE_BYTES) {
            setStatus("The file is larger than 16 MiB.");
            return;
        }
        String name=file.getName();
        if (name.toLowerCase(java.util.Locale.ROOT).endsWith(".svg")) {
            WebDom.readText(file, new WebDom.TextCallback() {
                public void accept(String value) {
                    if (requestId==loadSequence) {
                        loadSvg(file.getName(), value);
                    }
                }
            });
            return;
        }
        if (name.toLowerCase(java.util.Locale.ROOT).endsWith(".png")||name.toLowerCase(java.util.Locale.ROOT).endsWith(".jpg")||name.toLowerCase(java.util.Locale.ROOT).endsWith(".jpeg")) {
            WebDom.readDataUrl(file, new WebDom.TextCallback() {
                public void accept(String value) {
                    if (requestId!=loadSequence) {
                        return;
                    }
                    WebDom.readRaster(value, new WebDom.RasterCallback() {
                        public void accept(int width, int height, WebDom.ImageData data) {
                            if (requestId==loadSequence) {
                                loadRaster(file.getName(), width, height, data);
                            }
                        }
                    });
                }
            });
            return;
        }
        setStatus("Unsupported file type.");
    }
    private void loadSvg(String name, String value) {
        try {
            current=new SvgParser().parse(name, value);
            setStatus("SVG loaded. Ready to vectorize.");
        } catch (SvgParseException exception) {
            setStatus("Could not read SVG: "+exception.getMessage());
        }
    }
    private void loadRaster(String name, int width, int height, WebDom.ImageData data) {
        if (width<=0||height<=0||data==null) {
            setStatus("Could not decode the image.");
            return;
        }
        long length=(long)width*(long)height;
        if (length>MAX_PIXELS) {
            setStatus("The image is too large.");
            return;
        }
        int[] pixels=new int[(int)length];
        for (int index=0;index<pixels.length;index++) {
            int offset=index*4;
            int red=data.get(offset);
            int green=data.get(offset+1);
            int blue=data.get(offset+2);
            int alpha=data.get(offset+3);
            pixels[index]=(alpha<<24)|(red<<16)|(green<<8)|blue;
        }
        current=new VectorDocument(name, new RasterOrigin(name), Collections.<VectorPath>emptyList(), width, height, pixels);
        setStatus("Image loaded. Ready to vectorize.");
    }
    private void vectorize() {
        if (current==null) {
            setStatus("Choose a file first.");
            return;
        }
        setStatus("Vectorizing...");
        try {
            SvgCache cache=new SvgCache();
            StageRegistry registry=new StageRegistry(new WebLogger(), new BoundedPipelineMemoizer(), cache);
            PipelineConfig config=PipelineConfig.defaults().withRasterMode(RasterMode.parse(modeInput.getValue()));
            Pipeline pipeline=registry.buildPipeline(config);
            VectorDocument result=pipeline.run(current, config);
            String svg=cache.get(result);
            clearPreview();
            previewUrl=WebDom.createObjectUrl(svg);
            preview.setSrc(previewUrl);
            download.setHref(previewUrl);
            download.setDownload("anlyflad.svg");
            setStatus("Vectorization complete: "+result.getPaths().size()+" paths.");
        } catch (StageException exception) {
            setStatus("Vectorization failed: "+exception.getMessage());
        } catch (IllegalArgumentException exception) {
            setStatus("Vectorization failed: "+exception.getMessage());
        } catch (RuntimeException exception) {
            setStatus("Vectorization failed.");
        }
    }
    private void clearPreview() {
        if (previewUrl!=null) {
            WebDom.revokeObjectUrl(previewUrl);
            previewUrl=null;
        }
        preview.setSrc("");
        download.setHref("");
        download.setDownload("");
    }
    private WebDom.Element require(String id) {
        WebDom.Element element=document.getElementById(id);
        if (element==null) {
            throw new IllegalStateException("Missing web element: "+id);
        }
        return element;
    }
    private void setStatus(String value) {
        status.setTextContent(value);
    }
    private final class WebLogger implements PipelineLogger {
        public void onStage(String stageName, com.vectorium.core.stage.StageResult result, long nanos) {
            setStatus(stageName+" · "+result.name());
        }
    }
}
