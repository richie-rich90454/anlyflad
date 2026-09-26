package com.anlyflad.web;
import com.anlyflad.core.model.RasterOrigin;
import com.anlyflad.core.model.VectorDocument;
import com.anlyflad.core.model.VectorPath;
import com.anlyflad.core.stage.BoundedPipelineMemoizer;
import com.anlyflad.core.stage.Pipeline;
import com.anlyflad.core.stage.PipelineConfig;
import com.anlyflad.core.stage.PipelineLogger;
import com.anlyflad.core.stage.RasterMode;
import com.anlyflad.core.stage.StageException;
import com.anlyflad.core.stage.StageRegistry;
import com.anlyflad.core.stage.VectorMode;
import com.anlyflad.core.svg.SvgCache;
import com.anlyflad.core.svg.SvgParseException;
import com.anlyflad.core.svg.SvgParser;
import java.util.Collections;
public final class VectoriumWeb {
    private static final long MAX_FILE_BYTES=1024L*1024L*1024L;
    private static final long MAX_SVG_FILE_BYTES=1024L*1024L*1024L;
    private static final long MAX_PIXELS=100L*1024L*1024L;
    private WebDom.Document document;
    private WebDom.Element fileInput;
    private WebDom.Element modeInput;
    private WebDom.Element vectorModeInput;
    private WebDom.Element scaleInput;
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
        vectorModeInput=require("vector-mode-input");
        scaleInput=require("scale-input");
        runButton=require("run-button");
        preview=require("preview");
        download=require("download");
        status=require("status");
        setStatus("Choose a PNG, JPEG, or SVG file. Image files up to 1 GiB; SVG files up to 1 GiB; raster images up to 100 megapixels.");
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
        if (!Double.isFinite(fileSize)||fileSize<0.0) {
            setStatus("The file size is invalid.");
            return;
        }
        if (fileSize>MAX_FILE_BYTES) {
            setStatus("The file is larger than 1 GiB.");
            return;
        }
        String name=file.getName();
        String lowerName=name==null?null:name.toLowerCase(java.util.Locale.ROOT);
        if (lowerName==null) {
            setStatus("The file name is invalid.");
            return;
        }
        if (lowerName.endsWith(".svg")) {
            if (fileSize>MAX_SVG_FILE_BYTES) {
                setStatus("SVG files are limited to 1 GiB in the browser.");
                return;
            }
            WebDom.readText(file, new WebDom.TextCallback() {
                public void accept(String value, String error) {
                    if (requestId!=loadSequence) {
                        return;
                    }
                    if (error!=null&&error.length()>0) {
                        setStatus(error);
                    } else if (value==null||value.length()==0) {
                        setStatus("The SVG file is empty or could not be read.");
                    } else {
                        loadSvg(name, value);
                    }
                }
            });
            return;
        }
        if (lowerName.endsWith(".png")||lowerName.endsWith(".jpg")||lowerName.endsWith(".jpeg")) {
            WebDom.readRaster(file, (int)MAX_PIXELS, new WebDom.RasterCallback() {
                public void accept(int width, int height, WebDom.ImageData data, int sourceWidth, int sourceHeight, String error) {
                    if (requestId==loadSequence) {
                        if (error!=null&&error.length()>0) {
                            setStatus(error);
                        } else {
                            loadRaster(name, width, height, data, sourceWidth, sourceHeight);
                        }
                    }
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
    private void loadRaster(String name, int width, int height, WebDom.ImageData data, int sourceWidth, int sourceHeight) {
        if (width<=0||height<=0||data==null) {
            setStatus("Could not decode the image.");
            return;
        }
        long length=(long)width*(long)height;
        if (length>MAX_PIXELS||length>Integer.MAX_VALUE) {
            setStatus("The image exceeds the 100-megapixel limit.");
            return;
        }
        if (length>Integer.MAX_VALUE/4L) {
            setStatus("The image is too large for the browser pixel buffer.");
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
        current=VectorDocument.fromOwnedPixels(name, new RasterOrigin(name), Collections.<VectorPath>emptyList(), width, height, pixels);
        if (sourceWidth>0&&sourceHeight>0&&(sourceWidth!=width||sourceHeight!=height)) {
            setStatus("Image loaded at "+width+"x"+height+" (source "+sourceWidth+"x"+sourceHeight+"). Ready to vectorize.");
        } else {
            setStatus("Image loaded. Ready to vectorize.");
        }
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
            String quality=scaleInput.getValue();
            if (!"draft".equals(quality)&&!"balanced".equals(quality)&&!"max".equals(quality)) {
                quality="balanced";
            }
            PipelineConfig config=PipelineConfig.defaults().withRasterMode(RasterMode.parse(modeInput.getValue())).withVectorMode(VectorMode.parse(vectorModeInput.getValue())).withStageValue("vectorize", "quality", quality);
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
        public void onStage(String stageName, com.anlyflad.core.stage.StageResult result, long nanos) {
            setStatus(stageName+" · "+result.name());
        }
    }
}
