package com.vectorium.core.stage;
public final class StageException extends Exception {
    private static final long serialVersionUID=1L;
    private final boolean userError;
    public StageException(String message) {
        this(message, false, null);
    }
    public StageException(String message, Throwable cause) {
        this(message, false, cause);
    }
    private StageException(String message, boolean userError, Throwable cause) {
        super(message, cause);
        this.userError=userError;
    }
    public static StageException userError(String message) {
        return new StageException(message, true, null);
    }
    public static StageException userError(String message, Throwable cause) {
        return new StageException(message, true, cause);
    }
    public boolean isUserError() {
        return userError;
    }
}
