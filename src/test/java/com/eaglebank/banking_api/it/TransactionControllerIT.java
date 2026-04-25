package com.eaglebank.banking_api.it;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.eaglebank.banking_api.dto.request.CreateTransactionRequest;
import com.eaglebank.banking_api.entity.enums.Currency;
import com.eaglebank.banking_api.entity.enums.TransactionType;
import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

class TransactionControllerIT extends IntegrationUtils {

    @Nested
    class CreateTransaction {

        @Test
        void shouldDepositMoneyAndUpdateBalance() throws Exception {
            createUserAndGetId(TEST_EMAIL);
            String accessToken = loginAndGetAccessToken(TEST_EMAIL);
            String accountNumber = createAccount(accessToken);

            CreateTransactionRequest request = new CreateTransactionRequest(
                    new BigDecimal("100.50"), Currency.GBP, TransactionType.DEPOSIT, "Salary");

            mockMvc.perform(post(transactionsEndpoint(accountNumber))
                            .header("Authorization", "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(org.hamcrest.Matchers.matchesPattern("^tan-[A-Za-z0-9]+$")))
                    .andExpect(jsonPath("$.amount").value(100.50))
                    .andExpect(jsonPath("$.currency").value("GBP"))
                    .andExpect(jsonPath("$.type").value("deposit"))
                    .andExpect(jsonPath("$.reference").value("Salary"))
                    .andExpect(jsonPath("$.createdTimestamp").exists());

            mockMvc.perform(get(ACCOUNTS_ENDPOINT + "/" + accountNumber)
                            .header("Authorization", "Bearer " + accessToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.balance").value(100.50));
        }

        @Test
        void shouldWithdrawMoneyWhenSufficientFunds() throws Exception {
            createUserAndGetId(TEST_EMAIL);
            String accessToken = loginAndGetAccessToken(TEST_EMAIL);
            String accountNumber = createAccount(accessToken);

            CreateTransactionRequest request =
                    new CreateTransactionRequest(new BigDecimal("500.00"), Currency.GBP, TransactionType.DEPOSIT, null);
            mockMvc.perform(post(transactionsEndpoint(accountNumber))
                            .header("Authorization", "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated());

            CreateTransactionRequest withdrawal = new CreateTransactionRequest(
                    new BigDecimal("150.00"), Currency.GBP, TransactionType.WITHDRAWAL, null);

            mockMvc.perform(post(transactionsEndpoint(accountNumber))
                            .header("Authorization", "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(withdrawal)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.amount").value(150.00))
                    .andExpect(jsonPath("$.type").value("withdrawal"));

            mockMvc.perform(get(ACCOUNTS_ENDPOINT + "/" + accountNumber)
                            .header("Authorization", "Bearer " + accessToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.balance").value(350.00));
        }

        @Test
        void shouldReturnUnprocessableEntityWhenInsufficientFunds() throws Exception {
            createUserAndGetId(TEST_EMAIL);
            String accessToken = loginAndGetAccessToken(TEST_EMAIL);
            String accountNumber = createAccount(accessToken);

            CreateTransactionRequest withdrawal = new CreateTransactionRequest(
                    new BigDecimal("50.00"), Currency.GBP, TransactionType.WITHDRAWAL, null);

            mockMvc.perform(post(transactionsEndpoint(accountNumber))
                            .header("Authorization", "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(withdrawal)))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.message").value("Insufficient funds to process transaction"));

            mockMvc.perform(get(ACCOUNTS_ENDPOINT + "/" + accountNumber)
                            .header("Authorization", "Bearer " + accessToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.balance").value(0.00));
            assertThat(transactionRepository.count()).isZero();
        }

        @Test
        void shouldRejectConcurrentWithdrawalsThatWouldOverdraw() throws Exception {
            // With pessimistic locking, two concurrent £80 withdrawals on a £100
            // account must result in exactly one success and one insufficient-funds
            // rejection — never two successes.

            createUserAndGetId(TEST_EMAIL);
            String accessToken = loginAndGetAccessToken(TEST_EMAIL);
            String accountNumber = createAccount(accessToken);

            CreateTransactionRequest request =
                    new CreateTransactionRequest(new BigDecimal("100.00"), Currency.GBP, TransactionType.DEPOSIT, null);
            mockMvc.perform(post(transactionsEndpoint(accountNumber))
                            .header("Authorization", "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated());
            CreateTransactionRequest withdrawal = new CreateTransactionRequest(
                    new BigDecimal("80.00"), Currency.GBP, TransactionType.WITHDRAWAL, null);
            String body = objectMapper.writeValueAsString(withdrawal);

            ExecutorService pool = Executors.newFixedThreadPool(2);
            CountDownLatch start = new CountDownLatch(1);

            Callable<Integer> withdraw = () -> {
                start.await();
                return mockMvc.perform(post(transactionsEndpoint(accountNumber))
                                .header("Authorization", "Bearer " + accessToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body))
                        .andReturn()
                        .getResponse()
                        .getStatus();
            };

            Future<Integer> a = pool.submit(withdraw);
            Future<Integer> b = pool.submit(withdraw);
            start.countDown();

            int statusA = a.get(10, TimeUnit.SECONDS);
            int statusB = b.get(10, TimeUnit.SECONDS);
            pool.shutdown();

            assertThat(List.of(statusA, statusB)).containsExactlyInAnyOrder(201, 422);

            mockMvc.perform(get(ACCOUNTS_ENDPOINT + "/" + accountNumber)
                            .header("Authorization", "Bearer " + accessToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.balance").value(20.00));
        }

        @Test
        void shouldReturnForbiddenWhenTransactingOnAnotherUsersAccount() throws Exception {
            createUserAndGetId("user1@example.com");
            String user1Token = loginAndGetAccessToken("user1@example.com");
            String user1AccountNumber = createAccount(user1Token);

            createUserAndGetId("user2@example.com");
            String user2Token = loginAndGetAccessToken("user2@example.com");

            CreateTransactionRequest request =
                    new CreateTransactionRequest(new BigDecimal("100.00"), Currency.GBP, TransactionType.DEPOSIT, null);

            mockMvc.perform(post(transactionsEndpoint(user1AccountNumber))
                            .header("Authorization", "Bearer " + user2Token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.message").exists());
        }

        @Test
        void shouldReturnNotFoundWhenAccountDoesNotExist() throws Exception {
            createUserAndGetId(TEST_EMAIL);
            String accessToken = loginAndGetAccessToken(TEST_EMAIL);

            CreateTransactionRequest request =
                    new CreateTransactionRequest(new BigDecimal("100.00"), Currency.GBP, TransactionType.DEPOSIT, null);

            mockMvc.perform(post(transactionsEndpoint("01999999"))
                            .header("Authorization", "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value("Bank account was not found"));
        }

        @Test
        void shouldReturnBadRequestWhenAmountIsMissing() throws Exception {
            createUserAndGetId(TEST_EMAIL);
            String accessToken = loginAndGetAccessToken(TEST_EMAIL);
            String accountNumber = createAccount(accessToken);

            mockMvc.perform(post(transactionsEndpoint(accountNumber))
                            .header("Authorization", "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"currency\":\"GBP\",\"type\":\"deposit\"}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("Invalid details supplied"))
                    .andExpect(jsonPath("$.details[0].field").value("amount"));
        }

        @Test
        void shouldReturnBadRequestWhenAmountIsZero() throws Exception {
            createUserAndGetId(TEST_EMAIL);
            String accessToken = loginAndGetAccessToken(TEST_EMAIL);
            String accountNumber = createAccount(accessToken);

            CreateTransactionRequest request =
                    new CreateTransactionRequest(new BigDecimal("0.00"), Currency.GBP, TransactionType.DEPOSIT, null);

            mockMvc.perform(post(transactionsEndpoint(accountNumber))
                            .header("Authorization", "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.details[0].field").value("amount"));
        }

        @Test
        void shouldReturnBadRequestWhenAmountExceedsMaximum() throws Exception {
            createUserAndGetId(TEST_EMAIL);
            String accessToken = loginAndGetAccessToken(TEST_EMAIL);
            String accountNumber = createAccount(accessToken);

            CreateTransactionRequest request = new CreateTransactionRequest(
                    new BigDecimal("10001.00"), Currency.GBP, TransactionType.DEPOSIT, null);

            mockMvc.perform(post(transactionsEndpoint(accountNumber))
                            .header("Authorization", "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.details[0].field").value("amount"));
        }

        @Test
        void shouldReturnBadRequestWhenTypeIsMissing() throws Exception {
            createUserAndGetId(TEST_EMAIL);
            String accessToken = loginAndGetAccessToken(TEST_EMAIL);
            String accountNumber = createAccount(accessToken);

            mockMvc.perform(post(transactionsEndpoint(accountNumber))
                            .header("Authorization", "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"amount\":50.00,\"currency\":\"GBP\"}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("Invalid details supplied"))
                    .andExpect(jsonPath("$.details[0].field").value("type"));
        }

        @Test
        void shouldReturnBadRequestWhenCurrencyIsMissing() throws Exception {
            createUserAndGetId(TEST_EMAIL);
            String accessToken = loginAndGetAccessToken(TEST_EMAIL);
            String accountNumber = createAccount(accessToken);

            mockMvc.perform(post(transactionsEndpoint(accountNumber))
                            .header("Authorization", "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"amount\":50.00,\"type\":\"deposit\"}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("Invalid details supplied"))
                    .andExpect(jsonPath("$.details[0].field").value("currency"));
        }

        @Test
        void shouldReturnBadRequestWhenAccountNumberFormatIsInvalid() throws Exception {
            createUserAndGetId(TEST_EMAIL);
            String accessToken = loginAndGetAccessToken(TEST_EMAIL);

            CreateTransactionRequest request =
                    new CreateTransactionRequest(new BigDecimal("100.00"), Currency.GBP, TransactionType.DEPOSIT, null);

            mockMvc.perform(post(transactionsEndpoint("invalid-format"))
                            .header("Authorization", "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.details[0].field").value("accountNumber"));
        }

        @Test
        void shouldReturnUnauthorizedWhenNoToken() throws Exception {
            CreateTransactionRequest request =
                    new CreateTransactionRequest(new BigDecimal("100.00"), Currency.GBP, TransactionType.DEPOSIT, null);

            mockMvc.perform(post(transactionsEndpoint("01000001"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        void shouldReturnUnauthorizedWhenInvalidToken() throws Exception {
            CreateTransactionRequest request =
                    new CreateTransactionRequest(new BigDecimal("100.00"), Currency.GBP, TransactionType.DEPOSIT, null);

            mockMvc.perform(post(transactionsEndpoint("01000001"))
                            .header("Authorization", "Bearer invalid-token")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized());
        }
    }

    private String transactionsEndpoint(String accountNumber) {
        return ACCOUNTS_ENDPOINT + "/" + accountNumber + "/transactions";
    }
}
