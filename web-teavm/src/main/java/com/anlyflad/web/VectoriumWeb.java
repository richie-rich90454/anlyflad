package com.anlyflad.web;
import com.anlyflad.core.model.ParamSpec;
import com.anlyflad.core.model.ParamType;
import com.anlyflad.core.model.RasterOrigin;
import com.anlyflad.core.model.StageDescriptor;
import com.anlyflad.core.model.VectorDocument;
import com.anlyflad.core.model.VectorPath;
import com.anlyflad.core.stage.BoundedPipelineMemoizer;
import com.anlyflad.core.stage.Pipeline;
import com.anlyflad.core.stage.PipelineConfig;
import com.anlyflad.core.stage.PipelineLogger;
import com.anlyflad.core.stage.QualityScale;
import com.anlyflad.core.stage.RasterMode;
import com.anlyflad.core.stage.StageException;
import com.anlyflad.core.stage.StageRegistry;
import com.anlyflad.core.stage.StageResult;
import com.anlyflad.core.stage.VectorMode;
import com.anlyflad.core.svg.SvgCache;
import com.anlyflad.core.svg.SvgParseException;
import com.anlyflad.core.svg.SvgParser;
import java.util.Collections;
import java.util.List;
public final class VectoriumWeb {
    private static final long MAX_FILE_BYTES=1024L*1024L*1024L;
    private static final long MAX_SVG_FILE_BYTES=1024L*1024L*1024L;
    private static final long MAX_PIXELS=100L*1024L*1024L;
    private static final String INITIAL_STATUS="Choose a PNG, JPEG, or SVG file. Image files up to 1 GiB; SVG files up to 1 GiB; raster images up to 100 megapixels.";
    private static final String[] PRESETS={"default", "clean", "fast", "accurate"};
    private static final double ZOOM_STEP=1.25;
    private WebDom.Document document;
    private StageRegistry registry;
    private WebDom.Element fileInput;
    private WebDom.Element openButton;
    private WebDom.Element exportButton;
    private WebDom.Element runButton;
    private WebDom.Element fitButton;
    private WebDom.Element actualSizeButton;
    private WebDom.Element zoomInButton;
    private WebDom.Element zoomOutButton;
    private WebDom.Element zoomLabel;
    private WebDom.Element menuExit;
    private WebDom.Element modeInput;
    private WebDom.Element vectorModeInput;
    private WebDom.Element scaleInput;
    private WebDom.Element presetInput;
    private WebDom.Element cleanInput;
    private WebDom.Element outputScaleInput;
    private WebDom.Element preview;
    private WebDom.Element previewStage;
    private WebDom.Element download;
    private WebDom.Element status;
    private WebDom.Element detail;
    private WebDom.Element progress;
    private WebDom.Element progressFill;
    private WebDom.Element inspectorBody;
    private String previewUrl;
    private long loadSequence;
    private VectorDocument current;
    private boolean hasResult;
    public static void main(String[] args) {
        new VectoriumWeb().start();
    }
    public void start() {
        document=WebDom.document();
        registry=new StageRegistry();
        fileInput=require("file-input");
        openButton=require("open-button");
        exportButton=require("export-button");
        runButton=require("run-button");
        fitButton=require("fit-button");
        actualSizeButton=require("actual-button");
        zoomInButton=require("zoom-in-button");
        zoomOutButton=require("zoom-out-button");
        zoomLabel=require("zoom-label");
        menuExit=require("menu-exit");
        modeInput=require("mode-input");
        vectorModeInput=require("vector-mode-input");
        scaleInput=require("scale-input");
        presetInput=require("preset-input");
        cleanInput=require("clean-input");
        outputScaleInput=require("output-scale-input");
        preview=require("preview");
        previewStage=require("preview-stage");
        download=require("download");
        status=require("status");
        detail=require("detail");
        progress=require("progress");
        progressFill=require("progress-fill");
        inspectorBody=require("inspector-body");
        renderInspector();
        runButton.setDisabled(true);
        exportButton.setDisabled(true);
        setStatus(INITIAL_STATUS);
        setDetail("No image loaded");
        updateZoom(1.0);
        fileInput.addEventListener("change", new WebDom.EventListener() {
            public void handleEvent(WebDom.Event event) {
                readFile();
            }
        });
        preview.addEventListener("load", new WebDom.EventListener() {
            public void handleEvent(WebDom.Event event) {
                onPreviewLoad();
            }
        });
        onClick(openButton, new Handler() {
            public void run() {
                fileInput.click();
            }
        });
        onClick(runButton, new Handler() {
            public void run() {
                vectorize();
            }
        });
        onClick(exportButton, new Handler() {
            public void run() {
                export();
            }
        });
        onClick(menuExit, new Handler() {
            public void run() {
                setStatus("Close the tab (Ctrl+W) to exit.");
            }
        });
        onClick(fitButton, new Handler() {
            public void run() {
                fitPreview();
            }
        });
        onClick(actualSizeButton, new Handler() {
            public void run() {
                updateZoom(WebDom.previewActualSize(preview));
            }
        });
        onClick(zoomInButton, new Handler() {
            public void run() {
                updateZoom(WebDom.previewZoomBy(preview, ZOOM_STEP));
            }
        });
        onClick(zoomOutButton, new Handler() {
            public void run() {
                updateZoom(WebDom.previewZoomBy(preview, 1.0/ZOOM_STEP));
            }
        });
    }
    private void renderInspector() {
        StringBuilder html=new StringBuilder(8192);
        List<String> names=registry.names();
        html.append("<div class=\"stage-rows\" role=\"tablist\" aria-label=\"Pipeline stages\">");
        for (int index=0;index<names.size();index++) {
            String name=names.get(index);
            StageDescriptor descriptor=registry.getDescriptor(name);
            html.append("<button type=\"button\" class=\"stage-row");
            if (index==0) {
                html.append(" is-selected");
            }
            html.append("\" role=\"tab\" id=\"stage-row-").append(escape(name));
            html.append("\" data-stage=\"").append(escape(name));
            html.append("\" aria-controls=\"param-panel-").append(escape(name));
            html.append("\" aria-selected=\"").append(index==0?"true":"false");
            html.append("\">").append(escape(descriptor.getLabel())).append("</button>");
        }
        html.append("</div>");
        for (int index=0;index<names.size();index++) {
            String name=names.get(index);
            StageDescriptor descriptor=registry.getDescriptor(name);
            String escapedName=escape(name);
            html.append("<section class=\"param-panel\" role=\"tabpanel\" id=\"param-panel-").append(escapedName);
            html.append("\" data-panel=\"").append(escapedName);
            html.append("\" aria-labelledby=\"stage-row-").append(escapedName);
            html.append("\"");
            if (index!=0) {
                html.append(" hidden");
            }
            html.append(">");
            boolean locked=isProtectedStage(name);
            html.append("<label class=\"check param-enabled\" title=\"").append(locked?"This stage cannot be disabled.":"Enable or disable this stage.").append("\">");
            html.append("<input type=\"checkbox\" id=\"stage-enabled-").append(escapedName).append("\"");
            if (locked) {
                html.append(" checked disabled");
            } else {
                html.append(" checked");
            }
            html.append("> Stage enabled</label>");
            html.append("<p class=\"stage-desc\">").append(escape(descriptor.getDescription())).append("</p>");
            List<ParamSpec> parameters=descriptor.getParameters();
            if (parameters.isEmpty()) {
                html.append("<p class=\"param-hint\">This stage has no tunable parameters.</p>");
            }
            for (int parameterIndex=0;parameterIndex<parameters.size();parameterIndex++) {
                appendParameter(html, name, parameters.get(parameterIndex));
            }
            html.append("<div class=\"panel-footer\"><button type=\"button\" class=\"reset-button\">Reset to defaults</button></div>");
            html.append("</section>");
        }
        inspectorBody.setInnerHTML(html.toString());
    }
    private static void appendParameter(StringBuilder html, String stageName, ParamSpec parameter) {
        String id="param-"+stageName+"-"+parameter.getName();
        String label=escape(parameter.getLabel());
        String description=escape(parameter.getDescription());
        String defaultValue=escape(String.valueOf(parameter.getDefaultValue()));
        html.append("<div class=\"param-field\">");
        if (parameter.getType()==ParamType.BOOLEAN) {
            html.append("<label class=\"check param-check\" title=\"").append(description).append("\">");
            html.append("<input type=\"checkbox\" id=\"").append(escape(id)).append("\" data-default=\"").append(defaultValue).append("\"");
            if (Boolean.parseBoolean(String.valueOf(parameter.getDefaultValue()))) {
                html.append(" checked");
            }
            html.append("> ").append(label).append("</label>");
            html.append("<p class=\"param-hint\">Default ").append(defaultValue).append("</p>");
        } else {
            html.append("<label class=\"param-label\" for=\"").append(escape(id)).append("\" title=\"").append(description).append("\">").append(label).append("</label>");
            html.append("<input type=\"text\" id=\"").append(escape(id)).append("\" data-default=\"").append(defaultValue).append("\" value=\"").append(defaultValue).append("\" title=\"").append(description).append("\" autocomplete=\"off\" spellcheck=\"false\">");
            html.append("<p class=\"param-hint\">Default ").append(defaultValue);
            if (parameter.getMin()!=null||parameter.getMax()!=null) {
                html.append("  |  ").append(parameter.getMin()==null?"-":parameter.getMin().toString());
                html.append(" to ").append(parameter.getMax()==null?"-":parameter.getMax().toString());
            }
            html.append("</p>");
        }
        html.append("</div>");
    }
    private void readFile() {
        final long requestId=++loadSequence;
        current=null;
        clearPreview();
        runButton.setDisabled(true);
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
            setBusy(true);
            setStatus("Loading "+name+"...");
            setDetail("Loading "+name);
            WebDom.readText(file, new WebDom.TextCallback() {
                public void accept(String value, String error) {
                    if (requestId!=loadSequence) {
                        return;
                    }
                    if (error!=null&&error.length()>0) {
                        setBusy(false);
                        setStatus(error);
                    } else if (value==null||value.length()==0) {
                        setBusy(false);
                        setStatus("The SVG file is empty or could not be read.");
                    } else {
                        loadSvg(name, value);
                    }
                }
            });
            return;
        }
        if (lowerName.endsWith(".png")||lowerName.endsWith(".jpg")||lowerName.endsWith(".jpeg")) {
            setBusy(true);
            setStatus("Loading "+name+"...");
            setDetail("Loading "+name);
            WebDom.readRaster(file, (int)MAX_PIXELS, new WebDom.RasterCallback() {
                public void accept(int width, int height, WebDom.ImageData data, int sourceWidth, int sourceHeight, String error) {
                    if (requestId==loadSequence) {
                        if (error!=null&&error.length()>0) {
                            setBusy(false);
                            setStatus(error);
                        } else {
                            loadRaster(name, width, height, data, sourceWidth, sourceHeight, file);
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
        } catch (SvgParseException exception) {
            setBusy(false);
            setStatus("Could not read SVG: "+message(exception));
            return;
        }
        showSvg(value);
        setBusy(false);
        runButton.setDisabled(false);
        setDetail(current.getWidth()+" x "+current.getHeight());
        setStatus("SVG loaded. Ready to vectorize.");
    }
    private void loadRaster(String name, int width, int height, WebDom.ImageData data, int sourceWidth, int sourceHeight, WebDom.File file) {
        if (width<=0||height<=0||data==null) {
            setBusy(false);
            setStatus("Could not decode the image.");
            return;
        }
        long length=(long)width*(long)height;
        if (length>MAX_PIXELS||length>Integer.MAX_VALUE) {
            setBusy(false);
            setStatus("The image exceeds the 100-megapixel limit.");
            return;
        }
        if (length>Integer.MAX_VALUE/4L) {
            setBusy(false);
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
        clearPreview();
        previewUrl=WebDom.objectUrl(file);
        preview.setSrc(previewUrl);
        setBusy(false);
        runButton.setDisabled(false);
        setDetail(width+" x "+height);
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
        final PipelineConfig config;
        try {
            config=buildConfig();
        } catch (IllegalArgumentException exception) {
            setStatus("Vectorization failed: "+message(exception));
            return;
        }
        setBusy(true);
        setStatus("Vectorizing...");
        setDetail("Vectorizing "+current.getOrigin().getSourceName());
        WebDom.defer(new WebDom.Callback() {
            public void run() {
                runPipeline(config);
            }
        });
    }
    private PipelineConfig buildConfig() {
        int quality;
        try {
            quality=QualityScale.parse(scaleInput.getValue());
        } catch (IllegalArgumentException exception) {
            quality=QualityScale.DEFAULT;
        }
        String preset=presetInput.getValue();
        if (!isKnownPreset(preset)) {
            preset="default";
        }
        String outputScaleText=outputScaleInput.getValue();
        double outputScale;
        try {
            outputScale=Double.parseDouble(outputScaleText==null?"":outputScaleText.trim());
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Output scale must be a number between 0.1 and 16.");
        }
        if (!Double.isFinite(outputScale)||outputScale<0.1||outputScale>16.0) {
            throw new IllegalArgumentException("Output scale must be between 0.1 and 16.");
        }
        RasterMode rasterMode=rasterMode(modeInput.getValue());
        VectorMode vectorMode=vectorMode(vectorModeInput.getValue());
        PipelineConfig config=PipelineConfig.defaults().withPreset(preset).withClean(cleanInput.isChecked()).withRasterMode(rasterMode).withVectorMode(vectorMode);
        config=applyInspector(config);
        return config.withStageValue("vectorize", "mode", vectorMode.getOptionName()).withStageValue("vectorize", "quality", Integer.toString(quality)).withStageValue("quantize", "maxColors", Integer.toString(QualityScale.maxColors(quality))).withStageValue("vectorize", "outputScale", Double.toString(outputScale));
    }
    private PipelineConfig applyInspector(PipelineConfig config) {
        PipelineConfig updated=config;
        List<String> names=registry.names();
        for (int index=0;index<names.size();index++) {
            String name=names.get(index);
            WebDom.Element enabledToggle=document.getElementById("stage-enabled-"+name);
            if (enabledToggle!=null&&!isProtectedStage(name)&&!enabledToggle.isChecked()) {
                updated=updated.withStageDisabled(name);
            }
            StageDescriptor descriptor=registry.getDescriptor(name);
            for (int parameterIndex=0;parameterIndex<descriptor.getParameters().size();parameterIndex++) {
                ParamSpec parameter=descriptor.getParameters().get(parameterIndex);
                WebDom.Element input=document.getElementById("param-"+name+"-"+parameter.getName());
                if (input==null) {
                    continue;
                }
                String value=parameter.getType()==ParamType.BOOLEAN?Boolean.toString(input.isChecked()):input.getValue();
                if (value==null) {
                    continue;
                }
                value=value.trim();
                if (value.isEmpty()) {
                    continue;
                }
                String defaultValue=parameter.getDefaultValue()==null?null:parameter.getDefaultValue().toString();
                if (defaultValue!=null&&defaultValue.equals(value)) {
                    continue;
                }
                String error=validateParameter(parameter, value);
                if (error!=null) {
                    throw new IllegalArgumentException(error);
                }
                updated=updated.withStageValue(name, parameter.getName(), value);
            }
        }
        return updated;
    }
    private static String validateParameter(ParamSpec parameter, String value) {
        if (parameter.getType()==ParamType.BOOLEAN) {
            if (!value.equalsIgnoreCase("true")&&!value.equalsIgnoreCase("false")) {
                return "Stage parameter must be true or false: "+parameter.getName();
            }
            return null;
        }
        double numeric=0.0;
        if (parameter.getType()==ParamType.INTEGER) {
            try {
                numeric=Integer.parseInt(value);
            } catch (NumberFormatException exception) {
                return "Stage parameter must be an integer: "+parameter.getName();
            }
        } else if (parameter.getType()==ParamType.DOUBLE) {
            try {
                numeric=Double.parseDouble(value);
            } catch (NumberFormatException exception) {
                return "Stage parameter must be numeric: "+parameter.getName();
            }
            if (!Double.isFinite(numeric)) {
                return "Stage parameter must be finite: "+parameter.getName();
            }
        }
        if (parameter.getMin()!=null&&numeric<parameter.getMin().doubleValue()) {
            return "Stage parameter is below its minimum: "+parameter.getName();
        }
        if (parameter.getMax()!=null&&numeric>parameter.getMax().doubleValue()) {
            return "Stage parameter exceeds its maximum: "+parameter.getName();
        }
        return null;
    }
    private void runPipeline(PipelineConfig config) {
        try {
            SvgCache cache=new SvgCache();
            StageRegistry runRegistry=new StageRegistry(new WebLogger(), new BoundedPipelineMemoizer(), cache);
            Pipeline pipeline=runRegistry.buildPipeline(config);
            VectorDocument result=pipeline.run(current, config);
            String svg=cache.get(result);
            showSvg(svg);
            setDetail(result.getWidth()+" x "+result.getHeight());
            setStatus("Vectorization complete: "+result.getPaths().size()+" paths.");
        } catch (StageException exception) {
            setStatus("Vectorization failed: "+message(exception));
        } catch (IllegalArgumentException exception) {
            setStatus("Vectorization failed: "+message(exception));
        } catch (RuntimeException exception) {
            setStatus("Vectorization failed.");
        } finally {
            setBusy(false);
        }
    }
    private void export() {
        if (!hasResult) {
            setStatus("Vectorize the document before exporting.");
            return;
        }
        download.click();
        setStatus("Exported anlyflad.svg.");
    }
    private void fitPreview() {
        updateZoom(WebDom.fitPreview(preview, previewStage));
    }
    private void onPreviewLoad() {
        fitPreview();
    }
    private void updateZoom(double zoom) {
        long percent=Double.isFinite(zoom)?Math.round(zoom*100.0):100L;
        zoomLabel.setTextContent(percent+"%");
    }
    private void showSvg(String svg) {
        clearPreview();
        previewUrl=WebDom.createObjectUrl(svg);
        preview.setSrc(previewUrl);
        download.setHref(previewUrl);
        download.setDownload("anlyflad.svg");
        hasResult=true;
        exportButton.setDisabled(false);
    }
    private void clearPreview() {
        if (previewUrl!=null) {
            WebDom.revokeObjectUrl(previewUrl);
            previewUrl=null;
        }
        preview.setSrc("");
        download.setHref("");
        download.setDownload("");
        hasResult=false;
        exportButton.setDisabled(true);
    }
    private void setBusy(boolean busy) {
        progress.setClassName(busy?"progress is-busy":"progress");
        progress.setAttribute("aria-valuetext", busy?"Working":"Idle");
        if (!busy) {
            WebDom.setWidthPercent(progressFill, 0);
            progress.setAttribute("aria-valuenow", "0");
        }
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
        status.setAttribute("title", value);
    }
    private void setDetail(String value) {
        detail.setTextContent(value);
    }
    private static RasterMode rasterMode(String value) {
        try {
            return RasterMode.parse(value);
        } catch (IllegalArgumentException exception) {
            return RasterMode.COLOR;
        }
    }
    private static VectorMode vectorMode(String value) {
        try {
            return VectorMode.parse(value);
        } catch (IllegalArgumentException exception) {
            return VectorMode.CURVE;
        }
    }
    private static boolean isKnownPreset(String preset) {
        if (preset==null) {
            return false;
        }
        for (int index=0;index<PRESETS.length;index++) {
            if (PRESETS[index].equals(preset)) {
                return true;
            }
        }
        return false;
    }
    private static boolean isProtectedStage(String stageName) {
        return "validate".equals(stageName)||"vectorize".equals(stageName)||"serialize".equals(stageName);
    }
    private static String message(Throwable throwable) {
        String message=throwable.getMessage();
        return message==null||message.trim().isEmpty()?"unknown error":message;
    }
    private static String escape(String value) {
        StringBuilder escaped=new StringBuilder(value.length()+16);
        for (int index=0;index<value.length();index++) {
            char character=value.charAt(index);
            if (character=='&') {
                escaped.append("&amp;");
            } else if (character=='<') {
                escaped.append("&lt;");
            } else if (character=='>') {
                escaped.append("&gt;");
            } else if (character=='"') {
                escaped.append("&quot;");
            } else if (character=='\'') {
                escaped.append("&#39;");
            } else {
                escaped.append(character);
            }
        }
        return escaped.toString();
    }
    private void onClick(WebDom.Element element, final Handler handler) {
        element.addEventListener("click", new WebDom.EventListener() {
            public void handleEvent(WebDom.Event event) {
                handler.run();
            }
        });
    }
    private interface Handler {
        void run();
    }
    private final class WebLogger implements PipelineLogger {
        private int completed;
        public void onStage(String stageName, StageResult result, long nanos) {
            completed+=1;
            int percent=Math.min(100, (completed*100)/registry.names().size());
            setStatus(stageName+" · "+result.name());
            WebDom.setWidthPercent(progressFill, percent);
            progress.setAttribute("aria-valuenow", Integer.toString(percent));
        }
    }
}
