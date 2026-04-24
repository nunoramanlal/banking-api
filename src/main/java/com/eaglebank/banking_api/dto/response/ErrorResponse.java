package com.eaglebank.banking_api.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "ErrorResponse", description = "Error response payload")
public record ErrorResponse(
        @Schema(description = "Error message", example = "An unexpected error occurred")
        String message) {}
