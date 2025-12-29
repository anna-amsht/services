package com.innowise.paymentservice.exceptions;

import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@Slf4j
@RestControllerAdvice
public class CustomExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ErrorDTO> handleBadRequestException(BadRequestException ex) {
        log.error("BadRequestException: {}", ex.getMessage());
        return ResponseEntity.badRequest().body(ErrorDTO.builder()
                .error("Bad Request")
                .errorDescription(ex.getMessage())
                .build());
    }

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ErrorDTO> handleNotFoundException(NotFoundException ex) {
        log.error("NotFoundException: {}", ex.getMessage());
        return ResponseEntity.status(org.springframework.http.HttpStatus.NOT_FOUND).body(ErrorDTO.builder()
                .error("Not Found")
                .errorDescription(ex.getMessage())
                .build());
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorDTO> handleConstraintViolationException(ConstraintViolationException ex) {
        log.error("ConstraintViolationException: {}", ex.getMessage());
        return ResponseEntity.badRequest().body(ErrorDTO.builder()
                .error("Validation Error")
                .errorDescription(ex.getMessage())
                .build());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorDTO> handleGenericException(Exception ex) {
        log.error("Unexpected exception occurred", ex);
        return ResponseEntity.status(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR).body(ErrorDTO.builder()
                .error("Internal Server Error")
                .errorDescription("An unexpected error occurred")
                .build());
    }
}

