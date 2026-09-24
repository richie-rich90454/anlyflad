package com.vectorium.core.stage;
public final class StageException extends Exception {
    private static final long serialVersionUID=1L;
    public StageException(String message) {
        super(message);
    }
    public StageException(String message, Throwable cause) {
        super(message, cause);
    }
}
