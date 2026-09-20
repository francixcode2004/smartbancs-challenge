package com.tcs.SmartBancsApp.controller;

import java.util.List;
import java.util.Map;
import jakarta.validation.ConstraintViolationException;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.TransactionTimedOutException;
import org.springframework.transaction.CannotCreateTransactionException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, String>> handleStatus(ResponseStatusException exception) {
        return ResponseEntity.status(exception.getStatusCode()).body(Map.of(
                "message", exception.getReason() == null ? "Solicitud rechazada" : exception.getReason()));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, String>> handleIntegrity() {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
                "message", "Conflicto de integridad: datos duplicados o registros relacionados con cuentas o movimientos"));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException exception) {
        List<String> errors = exception.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage()).toList();
        // Do not include rejected values, which may contain passwords.
        return ResponseEntity.badRequest().body(Map.of("message", "Datos invalidos", "errors", errors));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Map<String, String>> handleServiceValidation() {
        return ResponseEntity.badRequest().body(Map.of("message", "Datos invalidos"));
    }

    @ExceptionHandler({
            TransientDataAccessException.class,
            TransactionTimedOutException.class,
            CannotCreateTransactionException.class,
            DataAccessResourceFailureException.class
    })
    public ResponseEntity<Map<String, String>> handleTemporaryFailure() {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .header("Retry-After", "1")
                .body(Map.of("message", "Error temporal. Reintenta con el mismo Idempotency-Key"));
    }
}
