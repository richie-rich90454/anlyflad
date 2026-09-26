package com.anlyflad.web;
import org.teavm.jso.JSBody;
import org.teavm.jso.JSClass;
import org.teavm.jso.JSFunctor;
import org.teavm.jso.JSIndexer;
import org.teavm.jso.JSMethod;
import org.teavm.jso.JSObject;
import org.teavm.jso.JSProperty;
public final class WebDom {
    @JSClass(name="Document")
    public interface Document extends JSObject {
        @JSMethod
        Element getElementById(String id);
    }
    @JSClass(name="Element")
    public interface Element extends JSObject {
        @JSMethod
        void addEventListener(String type, EventListener listener);
        @JSMethod
        void click();
        @JSProperty
        void setTextContent(String value);
        @JSProperty
        void setInnerHTML(String value);
        @JSProperty
        void setSrc(String value);
        @JSProperty
        void setHref(String value);
        @JSProperty
        void setDownload(String value);
        @JSProperty
        void setDisabled(boolean value);
        @JSProperty
        void setClassName(String value);
        @JSProperty
        FileList getFiles();
        @JSProperty
        String getValue();
        @JSProperty
        boolean isChecked();
        @JSMethod
        void setAttribute(String name, String value);
    }
    @JSClass(name="EventListener")
    public interface EventListener extends JSObject {
        void handleEvent(Event event);
    }
    @JSClass(name="Event")
    public interface Event extends JSObject {
    }
    @JSClass(name="File")
    public interface File extends JSObject {
        @JSProperty
        String getName();
        @JSProperty
        double getSize();
        @JSProperty
        String getType();
    }
    @JSClass(name="FileList")
    public interface FileList extends JSObject {
        @JSProperty
        int getLength();
        @JSIndexer
        File get(int index);
    }
    @JSClass(name="ImageData")
    public interface ImageData extends JSObject {
        @JSIndexer
        int get(int index);
    }
    @JSFunctor
    public interface TextCallback extends JSObject {
        void accept(String value, String error);
    }
    @JSFunctor
    public interface RasterCallback extends JSObject {
        void accept(int width, int height, ImageData data, int sourceWidth, int sourceHeight, String error);
    }
    @JSFunctor
    public interface Callback extends JSObject {
        void run();
    }
    @JSBody(params={}, script="return document;")
    public static native Document document();
    @JSBody(params={"file","callback"}, script="var reader = new FileReader(); reader.onload = function(){callback(reader.result, '');}; reader.onerror = function(){callback('', 'Could not read the file.');}; reader.readAsText(file);")
    public static native void readText(File file, TextCallback callback);
    @JSBody(params={"file","maxPixels","callback"}, script="var source = URL.createObjectURL(file); var image = new Image(); image.onload = function(){var width = image.naturalWidth || image.width; var height = image.naturalHeight || image.height; URL.revokeObjectURL(source); if (!isFinite(width) || !isFinite(height) || width <= 0 || height <= 0 || Math.floor(width) !== width || Math.floor(height) !== height) { callback(0, 0, null, 0, 0, 'Could not determine the image dimensions.'); return; } if (width > 2147483647 || height > 2147483647 || width > maxPixels / height) { callback(0, 0, null, 0, 0, 'The image exceeds the 100-megapixel limit.'); return; } try { var canvas = document.createElement('canvas'); canvas.width = width; canvas.height = height; var context = canvas.getContext('2d'); context.drawImage(image, 0, 0); var data = context.getImageData(0, 0, width, height).data; callback(width, height, data, width, height, ''); } catch (error) { callback(0, 0, null, 0, 0, 'Could not decode the image within browser memory limits.'); } }; image.onerror = function(){ URL.revokeObjectURL(source); callback(0, 0, null, 0, 0, 'Could not decode the image.'); }; image.src = source;")
    public static native void readRaster(File file, int maxPixels, RasterCallback callback);
    @JSBody(params={"value"}, script="return encodeURIComponent(value);")
    public static native String encodeUri(String value);
    @JSBody(params={"value"}, script="var blob=new Blob([value],{type:'image/svg+xml'}); return URL.createObjectURL(blob);")
    public static native String createObjectUrl(String value);
    @JSBody(params={"file"}, script="return URL.createObjectURL(file);")
    public static native String objectUrl(File file);
    @JSBody(params={"value"}, script="URL.revokeObjectURL(value);")
    public static native void revokeObjectUrl(String value);
    @JSBody(params={"image","container"}, script="var width=image.naturalWidth||0; var height=image.naturalHeight||0; if (width<=0||height<=0) { return 1; } var availableWidth=(container.clientWidth||0)-32; var availableHeight=(container.clientHeight||0)-32; if (availableWidth<=0||availableHeight<=0) { return 1; } var zoom=Math.max(0.1, Math.min(16, Math.min(availableWidth/width, availableHeight/height))); image.style.maxWidth='none'; image.style.width=(width*zoom)+'px'; image.style.height=(height*zoom)+'px'; return zoom;")
    public static native double fitPreview(Element image, Element container);
    @JSBody(params={"image"}, script="var width=image.naturalWidth||0; if (width<=0) { return 1; } image.style.maxWidth='none'; image.style.width=width+'px'; image.style.height='auto'; return 1;")
    public static native double previewActualSize(Element image);
    @JSBody(params={"image","factor"}, script="var width=image.naturalWidth||0; if (width<=0) { return 1; } var current=image.clientWidth||width; var zoom=Math.max(0.1, Math.min(16, (current/width)*factor)); image.style.maxWidth='none'; image.style.width=(width*zoom)+'px'; image.style.height='auto'; return zoom;")
    public static native double previewZoomBy(Element image, double factor);
    @JSBody(params={"element","percent"}, script="element.style.width = percent + '%';")
    public static native void setWidthPercent(Element element, int percent);
    @JSBody(params={"callback"}, script="setTimeout(function(){callback();}, 0);")
    public static native void defer(Callback callback);
    private WebDom() {
    }
}
