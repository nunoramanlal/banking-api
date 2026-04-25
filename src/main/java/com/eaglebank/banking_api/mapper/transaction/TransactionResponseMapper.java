package com.eaglebank.banking_api.mapper.transaction;

import com.eaglebank.banking_api.dto.response.TransactionResponse;
import com.eaglebank.banking_api.entity.Transaction;
import org.springframework.stereotype.Component;

@Component
public class TransactionResponseMapper {

    public TransactionResponse toResponse(Transaction transaction, String userId) {
        return new TransactionResponse(
                transaction.getId(),
                transaction.getAmount(),
                transaction.getCurrency(),
                transaction.getType(),
                transaction.getReference(),
                userId,
                transaction.getCreatedTimestamp());
    }
}
