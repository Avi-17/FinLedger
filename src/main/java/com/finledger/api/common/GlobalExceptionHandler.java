package com.finledger.api.common;

import org.springframework.http.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

/** The only place that translates domain exceptions into HTTP status codes. */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<Map<String, String>> notFound(NotFoundException e) { return error(HttpStatus.NOT_FOUND, e); }

    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<Map<String, String>> forbidden(ForbiddenException e) { return error(HttpStatus.FORBIDDEN, e); }

    @ExceptionHandler(BusinessRuleException.class)
    public ResponseEntity<Map<String, String>> businessRule(BusinessRuleException e) { return error(HttpStatus.BAD_REQUEST, e); }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<Map<String, String>> conflict(ConflictException e) { return error(HttpStatus.CONFLICT, e); }

    @ExceptionHandler(AuthenticationFailedException.class)
    public ResponseEntity<Map<String, String>> authFailed(AuthenticationFailedException e) { return error(HttpStatus.UNAUTHORIZED, e); }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidation(MethodArgumentNotValidException e) {
        String msg = e.getBindingResult().getFieldErrors().stream()
                .map(f -> f.getField() + ": " + f.getDefaultMessage())
                .findFirst().orElse("Invalid request");
        return ResponseEntity.badRequest().body(Map.of("error", msg));
    }

    private static ResponseEntity<Map<String, String>> error(HttpStatus status, Exception e) {
        return ResponseEntity.status(status).body(Map.of("error", e.getMessage()));
    }
}
