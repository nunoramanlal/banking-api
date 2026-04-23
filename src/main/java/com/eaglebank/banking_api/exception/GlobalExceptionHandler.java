package com.eaglebank.banking_api.exception;

import com.eaglebank.banking_api.dto.response.BadRequestErrorResponse;
import com.eaglebank.banking_api.dto.response.ErrorResponse;
import com.eaglebank.banking_api.dto.response.ValidationError;
import com.eaglebank.banking_api.dto.response.ValidationErrorType;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public BadRequestErrorResponse handleValidationException(MethodArgumentNotValidException ex) {
        List<ValidationError> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> new ValidationError(
                        error.getField(),
                        error.getDefaultMessage(),
                        error.getRejectedValue() == null
                                ? ValidationErrorType.MISSING
                                : ValidationErrorType.INVALID_FORMAT))
                .toList();

        return new BadRequestErrorResponse("Invalid details supplied", errors);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public BadRequestErrorResponse handleHttpMessageNotReadable(HttpMessageNotReadableException ex) {
        return new BadRequestErrorResponse("Request body is missing or malformed", List.of());
    }

    @ExceptionHandler(DataAccessException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ErrorResponse handleDataAccessException(DataAccessException ex) {
        log.error("Database error occurred", ex);
        return new ErrorResponse("A database error occurred");
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ErrorResponse handleGeneralException(Exception ex) {
        log.error("Unexpected error occurred", ex);
        return new ErrorResponse("An unexpected error occurred");
    }
}
