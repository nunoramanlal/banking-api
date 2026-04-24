package com.eaglebank.banking_api.dto.request;

import com.eaglebank.banking_api.dto.AddressDto;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;

@Schema(name = "UpdateUserRequest", description = "Request payload for updating a user")
public record UpdateUserRequest(
        @Schema(description = "Name of the user", example = "Test user")
        String name,

        @Schema(description = "User's address details") @Valid
        AddressDto address,

        @Schema(description = "User phone number", example = "+123456789123")
        @Pattern(regexp = "^\\+[1-9]\\d{1,14}$", message = "Phone number format is invalid")
        String phoneNumber,

        @Schema(description = "User email address", example = "test.user@example.com")
        @Email(message = "Email format is invalid")
        String email) {}
