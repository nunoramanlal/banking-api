package com.eaglebank.banking_api.dto.response;

import com.eaglebank.banking_api.dto.AddressDto;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "UserResponse", description = "Response payload for user data")
public record UserResponse(
        @Schema(description = "Unique identifier of the user", example = "123e4567-e89b-12d3-a456-426614174000")
        String id,

        @Schema(description = "Name of the user", example = "Test user")
        String name,

        @Schema(description = "User's address details") AddressDto address,

        @Schema(description = "User phone number", example = "+123456789123")
        String phoneNumber,

        @Schema(description = "User email address", example = "test.user@example.com")
        String email,

        @Schema(description = "Timestamp when the user was created", example = "2023-01-01T00:00:00")
        String createdTimestamp,

        @Schema(description = "Timestamp when the user was last updated", example = "2023-01-01T00:00:00")
        String updatedTimestamp) {}
