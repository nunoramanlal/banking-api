package com.eaglebank.banking_api.service.command;

import com.eaglebank.banking_api.entity.enums.Currency;
import com.eaglebank.banking_api.entity.enums.TransactionType;
import java.math.BigDecimal;

public record CreateTransactionCommand(BigDecimal amount, Currency currency, TransactionType type, String reference) {}
