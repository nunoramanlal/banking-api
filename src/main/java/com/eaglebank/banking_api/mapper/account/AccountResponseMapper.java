package com.eaglebank.banking_api.mapper.account;

import com.eaglebank.banking_api.dto.response.BankAccountResponse;
import com.eaglebank.banking_api.entity.Account;
import org.springframework.stereotype.Component;

@Component
public class AccountResponseMapper {

    public BankAccountResponse toResponse(Account account) {
        return new BankAccountResponse(
                String.format("%08d", account.getAccountNumber()),
                account.getSortCode(),
                account.getName(),
                account.getAccountType(),
                account.getBalance(),
                account.getCurrency(),
                account.getCreatedTimestamp(),
                account.getUpdatedTimestamp());
    }
}
