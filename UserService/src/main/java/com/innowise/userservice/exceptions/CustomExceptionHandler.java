package com.innowise.userservice.exceptions;

import jakarta.validation.ConstraintViolationException;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;


@Log4j2
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

    @ExceptionHandler(DuplicateException.class)
    public ResponseEntity<ErrorDTO> handleDuplicateException(DuplicateException ex) {
        log.error("DuplicateException: {}", ex.getMessage());
        return ResponseEntity.status(org.springframework.http.HttpStatus.CONFLICT).body(ErrorDTO.builder()
                .error("Conflict")
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

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorDTO> handleAccessDeniedException(AccessDeniedException ex) {
        log.error("AccessDeniedException: {}", ex.getMessage());
        return ResponseEntity.status(org.springframework.http.HttpStatus.FORBIDDEN).body(ErrorDTO.builder()
                .error("Forbidden")
                .errorDescription(ex.getMessage())
                .build());
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorDTO> handleAuthenticationException(AuthenticationException ex) {
        log.error("AuthenticationException: {}", ex.getMessage());
        return ResponseEntity.status(org.springframework.http.HttpStatus.UNAUTHORIZED).body(ErrorDTO.builder()
                .error("Unauthorized")
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
