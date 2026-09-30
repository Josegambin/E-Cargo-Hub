package com.ecargohub.backend.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.OffsetDateTime;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // ---------- 404 ----------

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(ResourceNotFoundException ex, HttpServletRequest req) {
        log.warn("404 Not Found: {} → {}", req.getRequestURI(), ex.getMessage());
        return build(HttpStatus.NOT_FOUND, "NOT_FOUND", ex.getMessage(), req);
    }

    // ---------- 409 CONFLICT ----------

    @ExceptionHandler(VehicleAlreadyRunningException.class)
    public ResponseEntity<Map<String, Object>> handleAlreadyRunning(VehicleAlreadyRunningException ex,
            HttpServletRequest req) {
        log.warn("409 Conflict: {} → {}", req.getRequestURI(), ex.getMessage());
        return build(HttpStatus.CONFLICT, "CONFLICT", ex.getMessage(), req);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalState(IllegalStateException ex, HttpServletRequest req) {
        log.warn("409 Conflict (IllegalState): {} → {}", req.getRequestURI(), ex.getMessage());
        return build(HttpStatus.CONFLICT, "CONFLICT", ex.getMessage(), req);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, Object>> handleDataIntegrity(DataIntegrityViolationException ex,
            HttpServletRequest req) {
        log.warn("409 Conflict (DataIntegrity): {} → {}", req.getRequestURI(), ex.getMessage());
        return build(HttpStatus.CONFLICT, "CONFLICT",
                "Data integrity violation (probably a duplicate or invalid reference)", req);
    }

    // ---------- 400 BAD REQUEST ----------

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex,
            HttpServletRequest req) {
        String msg = ex.getBindingResult().getFieldErrors().stream()
                .map(f -> f.getField() + ": " + f.getDefaultMessage()).reduce((a, b) -> a + "; " + b)
                .orElse("Validation failed");
        log.warn("400 Bad Request: {} → {}", req.getRequestURI(), msg);
        return build(HttpStatus.BAD_REQUEST, "BAD_REQUEST", msg, req);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArg(IllegalArgumentException ex, HttpServletRequest req) {
        log.warn("400 Bad Request (IllegalArg): {} → {}", req.getRequestURI(), ex.getMessage());
        return build(HttpStatus.BAD_REQUEST, "BAD_REQUEST", ex.getMessage(), req);
    }

    // ---------- 500 ----------

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneric(Exception ex, HttpServletRequest req) {
        log.error("500 Internal Server Error on {}: {}", req.getRequestURI(), ex.getMessage(), ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_SERVER_ERROR", "Unexpected error. Check server logs.",
                req);
    }

    // ---------- Helper ----------

    private ResponseEntity<Map<String, Object>> build(HttpStatus status, String error, String message,
            HttpServletRequest req) {
        return ResponseEntity.status(status).body(Map.of("timestamp", OffsetDateTime.now(), "status", status.value(),
                "error", error, "message", message, "path", req.getRequestURI()));
    }
}