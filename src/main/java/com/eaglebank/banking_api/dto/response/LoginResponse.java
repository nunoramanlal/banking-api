package com.eaglebank.banking_api.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "LoginResponse", description = "Authentication response payload")
public record LoginResponse(
        @Schema(description = "JWT access token", example = "eyJhbGciOiJIUzI1NiJ9...")
        String accessToken,

        @Schema(
                description = "Refresh token used to obtain a new access token",
                example = "550e8400-e29b-41d4-a716-446655440000")
        String refreshToken,

        @Schema(description = "Token type", example = "Bearer")
        String tokenType,

        @Schema(description = "Access token lifetime in seconds", example = "900")
        long expiresIn) {}
