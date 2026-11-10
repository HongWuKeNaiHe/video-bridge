package dev.videobridge.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BridgeException.class)
    public ResponseEntity<ApiResponse<Void>> onBridge(BridgeException e) {
        return ResponseEntity.ok(ApiResponse.fail(e.getCode(), e.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> onInvalidArg(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(ApiResponse.fail(e.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> onUnexpected(Exception e) {
        log.error("请求处理失败", e);
        return ResponseEntity.internalServerError().body(ApiResponse.fail(500, e.getMessage()));
    }
}
