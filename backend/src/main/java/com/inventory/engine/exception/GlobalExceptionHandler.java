package com.inventory.engine.exception;

import com.inventory.engine.dto.StockUpdateResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(StockConflictException.class)
    public ResponseEntity<StockUpdateResponse> handleConflict(StockConflictException ex) {
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(StockUpdateResponse.conflict(ex.getSku(), ex.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleBadRequest(IllegalArgumentException ex) {
        return ResponseEntity
                .badRequest()
                .body(Map.of("error", ex.getMessage()));
    }
}
