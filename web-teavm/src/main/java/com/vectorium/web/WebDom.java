package com.vectorium.web;
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
        int getSize();
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
        void accept(String value);
    }
    @JSFunctor
    public interface RasterCallback extends JSObject {
        void accept(int width, int height, ImageData data);
    }
    @JSBody(params={}, script="return document;")
    public static native Document document();
    @JSBody(params={"file","callback"}, script="var reader = new FileReader(); reader.onload = function(){callback(reader.result);}; reader.onerror = function(){callback('');}; reader.readAsText(file);")
    public static native void readText(File file, TextCallback callback);
    @JSBody(params={"file","callback"}, script="var reader = new FileReader(); reader.onload = function(){callback(reader.result);}; reader.onerror = function(){callback('');}; reader.readAsDataURL(file);")
    public static native void readDataUrl(File file, TextCallback callback);
    @JSBody(params={"source","callback"}, script="var image = new Image(); image.onload = function(){var canvas = document.createElement('canvas'); canvas.width = image.width; canvas.height = image.height; var context = canvas.getContext('2d'); context.drawImage(image, 0, 0); callback(image.width, image.height, context.getImageData(0, 0, image.width, image.height).data);}; image.onerror = function(){callback(0, 0, null);}; image.src = source;")
    public static native void readRaster(String source, RasterCallback callback);
    @JSBody(params={"value"}, script="return encodeURIComponent(value);")
    public static native String encodeUri(String value);
    private WebDom() {
    }
}
