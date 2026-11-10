package dev.videobridge.common;

public class BridgeException extends RuntimeException {

    private final int code;

    public BridgeException(String message) {
        this(1, message);
    }

    public BridgeException(int code, String message) {
        super(message);
        this.code = code;
    }

    public BridgeException(String message, Throwable cause) {
        super(message, cause);
        this.code = 1;
    }

    public int getCode() {
        return code;
    }
}
