package com.eaglebank.banking_api.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;

@Schema(name = "UpdateBankAccountRequest", description = "Request payload for updating a bank account")
public record UpdateBankAccountRequest(
        @Schema(
                description = "Display name of the account",
                example = "Personal Bank Account",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        String name,

        @Schema(
                description = "Type of the account",
                example = "personal",
                allowableValues = {"personal"},
                requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        @Pattern(regexp = "^personal$", message = "Account type must be 'personal'")
        String accountType) {}
