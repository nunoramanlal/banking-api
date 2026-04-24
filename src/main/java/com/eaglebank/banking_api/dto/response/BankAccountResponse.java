package com.eaglebank.banking_api.dto.response;

import com.eaglebank.banking_api.entity.Currency;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(name = "BankAccountResponse", description = "Bank account details returned to the client")
public record BankAccountResponse(
        @Schema(
                description = "Unique account number",
                example = "01000001",
                pattern = "^01\\d{6}$",
                requiredMode = Schema.RequiredMode.REQUIRED)
        String accountNumber,
        @Schema(
                description = "Sort code of the bank",
                example = "10-10-10",
                allowableValues = {"10-10-10"},
                requiredMode = Schema.RequiredMode.REQUIRED)
        String sortCode,
        @Schema(
                description = "Display name of the account",
                example = "Personal Bank Account",
                requiredMode = Schema.RequiredMode.REQUIRED)
        String name,
        @Schema(
                description = "Type of the account",
                example = "personal",
                allowableValues = {"personal"},
                requiredMode = Schema.RequiredMode.REQUIRED)
        String accountType,
        @Schema(
                description = "Current account balance (up to two decimal places)",
                example = "0.00",
                minimum = "0.00",
                maximum = "10000.00",
                requiredMode = Schema.RequiredMode.REQUIRED)
        BigDecimal balance,
        @Schema(
                description = "Currency of the account",
                example = "GBP",
                requiredMode = Schema.RequiredMode.REQUIRED)
        Currency currency,
        @Schema(
                description = "Timestamp when the account was created",
                example = "2026-01-15T10:30:00",
                requiredMode = Schema.RequiredMode.REQUIRED)
        LocalDateTime createdTimestamp,
        @Schema(
                description = "Timestamp when the account was last updated",
                example = "2026-01-16T11:45:00",
                requiredMode = Schema.RequiredMode.REQUIRED)
        LocalDateTime updatedTimestamp) {}