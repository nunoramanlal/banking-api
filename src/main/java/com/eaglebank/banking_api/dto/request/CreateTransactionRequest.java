package com.eaglebank.banking_api.dto.request;

import com.eaglebank.banking_api.entity.enums.Currency;
import com.eaglebank.banking_api.entity.enums.TransactionType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

@Schema(name = "CreateTransactionRequest", description = "Request payload for creating a transaction")
public record CreateTransactionRequest(
        @Schema(
                description = "Currency amount with up to two decimal places",
                example = "10.99",
                minimum = "0.01",
                maximum = "10000.00",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "Amount is required")
        @DecimalMin(value = "0.01", message = "Amount must be at least 0.01")
        @DecimalMax(value = "10000.00", message = "Amount must not exceed 10000.00")
        @Digits(integer = 5, fraction = 2, message = "Amount must have up to two decimal places")
        BigDecimal amount,

        @Schema(
                description = "Currency of the transaction",
                example = "GBP",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "Currency is required")
        Currency currency,

        @Schema(
                description = "Type of the transaction",
                example = "deposit",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "Type is required")
        TransactionType type,

        @Schema(description = "Optional reference for the transaction", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        String reference) {}
