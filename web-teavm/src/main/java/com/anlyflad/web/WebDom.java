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
    @JSBody(params={"value"}, script="URL.revokeObjectURL(value);")
    public static native void revokeObjectUrl(String value);
    private WebDom() {
    }
}
