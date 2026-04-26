package ru.iposhka.advice;

import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.Arrays;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import ru.iposhka.dto.response.ErrorResponseDto;
import ru.iposhka.exception.BadRequestException;
import ru.iposhka.exception.ConflictException;
import ru.iposhka.exception.NotFoundException;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponseDto> handleValidation(
            MethodArgumentNotValidException e,
            HttpServletRequest request
    ) {
        String[] messages = e.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(org.springframework.validation.FieldError::getDefaultMessage)
                .toArray(String[]::new);

        log.info("Validation failed: {}", Arrays.toString(messages));

        return ResponseEntity.badRequest().body(buildError(request, messages));
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ErrorResponseDto> handleBadRequest(BadRequestException e, HttpServletRequest request) {
        log.info(e.getMessage());
        return ResponseEntity.badRequest().body(buildError(request, e.getMessage()));
    }

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ErrorResponseDto> handleNotFound(NotFoundException e, HttpServletRequest request) {
        log.info(e.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(buildError(request, e.getMessage()));
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ErrorResponseDto> handleConflict(ConflictException e, HttpServletRequest request) {
        log.info(e.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(buildError(request, e.getMessage()));
    }

    private ErrorResponseDto buildError(HttpServletRequest request, String... message) {
        return new ErrorResponseDto(Instant.now().toString(), message, request.getRequestURI());
    }
}