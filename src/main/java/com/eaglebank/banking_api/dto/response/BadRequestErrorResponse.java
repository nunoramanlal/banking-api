package com.eaglebank.banking_api.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(name = "BadRequestErrorResponse", description = "Validation error response payload")
public record BadRequestErrorResponse(
        @Schema(description = "Error message", example = "Invalid details supplied")
        String message,

        @Schema(description = "List of validation errors") List<ValidationError> details) {}
