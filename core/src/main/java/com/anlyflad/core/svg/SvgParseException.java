package com.anlyflad.core.svg;
public final class SvgParseException extends Exception {
    private static final long serialVersionUID=1L;
    public SvgParseException(String message) {
        super(message);
    }
    public SvgParseException(String message, Throwable cause) {
        super(message, cause);
    }
}
