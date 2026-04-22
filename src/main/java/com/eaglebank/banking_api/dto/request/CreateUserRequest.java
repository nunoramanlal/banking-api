package com.eaglebank.banking_api.dto.request;

import com.eaglebank.banking_api.dto.AddressDto;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

@Schema(name = "CreateUserRequest", description = "Request payload for creating a new user")
public record CreateUserRequest(
        @Schema(description = "Name of the user", example = "Test user", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "Name is required")
        String name,

        @Schema(description = "User's address details", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        @NotNull(message = "Address is required")
        @Valid
        AddressDto address,

        @Schema(
                description = "User phone number",
                example = "+123456789123",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "Phone number is required")
        @Pattern(regexp = "^\\+[1-9]\\d{1,14}$", message = "Phone number format is invalid")
        String phoneNumber,

        @Schema(
                description = "User email address",
                example = "test.user@example.com",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "Email is required")
        @Email(message = "Email format is invalid")
        String email) {}
