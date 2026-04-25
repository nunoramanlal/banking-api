package com.eaglebank.banking_api.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

@Schema(name = "CreateBankAccountRequest", description = "Request payload for creating a new bank account")
public record CreateBankAccountRequest(
        @Schema(
                description = "Display name of the account",
                example = "Personal Bank Account",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "Name is required")
        String name,

        @Schema(
                description = "Type of the account",
                example = "personal",
                allowableValues = {"personal"},
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "Account type is required")
        @Pattern(regexp = "^personal$", message = "Account type must be 'personal'")
        String accountType) {}
