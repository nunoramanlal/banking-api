package com.eaglebank.banking_api.dto.response;

import com.eaglebank.banking_api.entity.enums.Currency;
import com.eaglebank.banking_api.entity.enums.TransactionType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(name = "TransactionResponse", description = "Transaction details returned to the client")
public record TransactionResponse(
        @Schema(
                description = "Unique transaction identifier",
                example = "tan-abc123",
                pattern = "^tan-[A-Za-z0-9]+$",
                requiredMode = Schema.RequiredMode.REQUIRED)
        String id,

        @Schema(
                description = "Currency amount with up to two decimal places",
                example = "10.99",
                minimum = "0.00",
                maximum = "10000.00",
                requiredMode = Schema.RequiredMode.REQUIRED)
        BigDecimal amount,

        @Schema(
                description = "Currency of the transaction",
                example = "GBP",
                requiredMode = Schema.RequiredMode.REQUIRED)
        Currency currency,

        @Schema(
                description = "Type of the transaction",
                example = "deposit",
                requiredMode = Schema.RequiredMode.REQUIRED)
        TransactionType type,

        @Schema(description = "Optional reference for the transaction", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        String reference,

        @Schema(
                description = "ID of the user who initiated the transaction",
                example = "usr-abc123",
                pattern = "^usr-[A-Za-z0-9]+$",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        String userId,

        @Schema(
                description = "Timestamp when the transaction was created",
                example = "2026-01-15T10:30:00",
                requiredMode = Schema.RequiredMode.REQUIRED)
        LocalDateTime createdTimestamp) {}
