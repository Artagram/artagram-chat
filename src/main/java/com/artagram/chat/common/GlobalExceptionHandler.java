package com.artagram.chat.common;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private final Counter http5xxCounter;

    public GlobalExceptionHandler(MeterRegistry meterRegistry) {
        this.http5xxCounter = Counter.builder("http_errores_5xx_total")
                .description("Errores 5xx en artagram-chat")
                .register(meterRegistry);
    }

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ErrorResponse> handleApiException(ApiException ex) {
        String traceId = MDC.get(TraceIdFilter.TRACE_ID_MDC_KEY);
        if (ex.getStatus().is5xxServerError()) {
            http5xxCounter.increment();
            log.error("Error de negocio: {}", ex.getMessage(), ex);
        } else {
            log.warn("Rechazo de negocio [{}]: {}", ex.getCode(), ex.getMessage());
        }
        return ResponseEntity.status(ex.getStatus()).body(ErrorResponse.of(ex.getCode(), ex.getMessage(), traceId));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        String traceId = MDC.get(TraceIdFilter.TRACE_ID_MDC_KEY);
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(fe -> fieldErrors.put(fe.getField(), fe.getDefaultMessage()));
        return ResponseEntity.badRequest().body(ErrorResponse.ofViolations(traceId, fieldErrors));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex) {
        String traceId = MDC.get(TraceIdFilter.TRACE_ID_MDC_KEY);
        http5xxCounter.increment();
        log.error("Excepción inesperada traceId={}", traceId, ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorResponse.of("ERROR_INTERNO", "Algo salio mal, intenta de nuevo", traceId));
    }
}
