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
import ru.iposhka.exception.BadJwtException;
import ru.iposhka.exception.UserAlreadyExistsException;
import ru.iposhka.exception.UserNotFoundException;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ErrorResponseDto> handleUserNotFound(UserNotFoundException e,
            HttpServletRequest request) {
        log.info(e.getMessage());

        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(buildError(HttpStatus.NOT_FOUND, request, e.getMessage()));
    }

    @ExceptionHandler(UserAlreadyExistsException.class)
    public ResponseEntity<ErrorResponseDto> handleUserAlreadyExists(UserAlreadyExistsException e,
            HttpServletRequest request) {
        log.info(e.getMessage());

        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(buildError(HttpStatus.CONFLICT, request, e.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponseDto> handleConstraintViolation(MethodArgumentNotValidException e,
            HttpServletRequest request) {
        String[] messages = e.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(org.springframework.validation.FieldError::getDefaultMessage)
                .toArray(String[]::new);

        log.info("Validation failed: {}", Arrays.toString(messages));

        return ResponseEntity.badRequest()
                .body(buildError(HttpStatus.BAD_REQUEST, request, messages));
    }

    @ExceptionHandler(BadJwtException.class)
    public ResponseEntity<ErrorResponseDto> handleJwt(BadJwtException e,
            HttpServletRequest request) {
        log.info("Jwt error: {}", e.getMessage());

        return ResponseEntity.badRequest()
                .body(buildError(HttpStatus.UNAUTHORIZED, request, e.getMessage()));
    }

    private ErrorResponseDto buildError(HttpStatus status, HttpServletRequest request, String... message) {
        return new ErrorResponseDto(
                Instant.now().toString(),
                status.value(),
                status.getReasonPhrase(),
                message,
                request.getRequestURI()
        );
    }
}