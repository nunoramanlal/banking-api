package com.eaglebank.banking_api.mapper.account;

import com.eaglebank.banking_api.dto.response.BankAccountResponse;
import com.eaglebank.banking_api.dto.response.ListBankAccountsResponse;
import com.eaglebank.banking_api.entity.Account;
import java.util.List;
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

    public ListBankAccountsResponse toListResponse(List<Account> accounts) {
        List<BankAccountResponse> responses =
                accounts.stream().map(this::toResponse).toList();
        return new ListBankAccountsResponse(responses);
    }
}
