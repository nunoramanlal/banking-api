package com.eaglebank.banking_api.mapper.account;

import static org.assertj.core.api.Assertions.assertThat;

import com.eaglebank.banking_api.dto.response.BankAccountResponse;
import com.eaglebank.banking_api.entity.Account;
import com.eaglebank.banking_api.entity.Currency;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class AccountResponseMapperTest {

    private final AccountResponseMapper mapper = new AccountResponseMapper();

    @Test
    void shouldMapAllFieldsFromAccountToBankAccountResponse() {
        LocalDateTime createdAt = LocalDateTime.of(2026, 1, 15, 10, 30);
        LocalDateTime updatedAt = LocalDateTime.of(2026, 1, 16, 11, 45);

        Account account = new Account();
        account.setAccountNumber(1000001L);
        account.setSortCode("10-10-10");
        account.setName("Personal Bank Account");
        account.setAccountType("personal");
        account.setBalance(new BigDecimal("500.00"));
        account.setCurrency(Currency.GBP);
        account.setCreatedTimestamp(createdAt);
        account.setUpdatedTimestamp(updatedAt);

        BankAccountResponse response = mapper.toResponse(account);

        assertThat(response.accountNumber()).isEqualTo("01000001");
        assertThat(response.sortCode()).isEqualTo("10-10-10");
        assertThat(response.name()).isEqualTo("Personal Bank Account");
        assertThat(response.accountType()).isEqualTo("personal");
        assertThat(response.balance()).isEqualByComparingTo(new BigDecimal("500.00"));
        assertThat(response.currency()).isEqualTo(Currency.GBP);
        assertThat(response.createdTimestamp()).isEqualTo(createdAt);
        assertThat(response.updatedTimestamp()).isEqualTo(updatedAt);
    }
}