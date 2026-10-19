package com.artagram.chat.common;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record ErrorResponse(
        String code,
        String message,
        String traceId,
        Instant timestamp,
        List<FieldViolation> violations
) {
    public record FieldViolation(String field, String message) {}

    public static ErrorResponse of(String code, String message, String traceId) {
        return new ErrorResponse(code, message, traceId, Instant.now(), List.of());
    }

    public static ErrorResponse ofViolations(String traceId, Map<String, String> fieldErrors) {
        List<FieldViolation> violations = fieldErrors.entrySet().stream()
                .map(e -> new FieldViolation(e.getKey(), e.getValue()))
                .toList();
        return new ErrorResponse("VALIDACION", "Hay campos con errores", traceId, Instant.now(), violations);
    }
}
