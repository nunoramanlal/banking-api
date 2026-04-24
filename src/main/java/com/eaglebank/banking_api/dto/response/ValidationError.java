package com.eaglebank.banking_api.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "ValidationError", description = "Individual field validation error")
public record ValidationError(
        @Schema(description = "Field that failed validation", example = "email")
        String field,

        @Schema(description = "Validation error message", example = "Email format is invalid")
        String message,

        @Schema(description = "Type of validation error", example = "INVALID_FORMAT")
        ValidationErrorType type) {}
