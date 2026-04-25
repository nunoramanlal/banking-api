package com.eaglebank.banking_api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.eaglebank.banking_api.entity.Account;
import com.eaglebank.banking_api.entity.Transaction;
import com.eaglebank.banking_api.entity.User;
import com.eaglebank.banking_api.entity.enums.Currency;
import com.eaglebank.banking_api.entity.enums.TransactionType;
import com.eaglebank.banking_api.exception.ForbiddenException;
import com.eaglebank.banking_api.exception.NotFoundException;
import com.eaglebank.banking_api.exception.UnprocessableEntityException;
import com.eaglebank.banking_api.repository.AccountRepository;
import com.eaglebank.banking_api.repository.TransactionRepository;
import com.eaglebank.banking_api.service.command.CreateTransactionCommand;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    private static final String USER_ID = "usr-abc123";
    private static final String OTHER_USER_ID = "usr-other999";
    private static final Long ACCOUNT_NUMBER = 1000001L;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private AccountRepository accountRepository;

    @InjectMocks
    private TransactionService transactionService;

    private User owner;
    private Account account;

    @BeforeEach
    void setUp() {
        owner = new User();
        owner.setId(USER_ID);

        account = new Account();
        account.setAccountNumber(ACCOUNT_NUMBER);
        account.setUser(owner);
        account.setCurrency(Currency.GBP);
        account.setBalance(new BigDecimal("100.00"));
    }

    @Test
    void shouldIncreaseBalanceByDepositAmount() {
        CreateTransactionCommand command =
                new CreateTransactionCommand(new BigDecimal("50.00"), Currency.GBP, TransactionType.DEPOSIT, "Salary");

        when(accountRepository.findByAccountNumberForUpdate(ACCOUNT_NUMBER)).thenReturn(Optional.of(account));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> inv.getArgument(0));

        transactionService.createTransaction(USER_ID, ACCOUNT_NUMBER, command);

        assertThat(account.getBalance()).isEqualByComparingTo(new BigDecimal("150.00"));
    }

    @Test
    void shouldPersistTransactionWithCorrectFields() {
        CreateTransactionCommand command =
                new CreateTransactionCommand(new BigDecimal("25.50"), Currency.GBP, TransactionType.DEPOSIT, "Refund");

        when(accountRepository.findByAccountNumberForUpdate(ACCOUNT_NUMBER)).thenReturn(Optional.of(account));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> inv.getArgument(0));

        transactionService.createTransaction(USER_ID, ACCOUNT_NUMBER, command);

        ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository).save(captor.capture());

        Transaction saved = captor.getValue();
        assertThat(saved.getAccount()).isEqualTo(account);
        assertThat(saved.getAmount()).isEqualByComparingTo(new BigDecimal("25.50"));
        assertThat(saved.getType()).isEqualTo(TransactionType.DEPOSIT);
        assertThat(saved.getReference()).isEqualTo("Refund");
        assertThat(saved.getCurrency()).isEqualTo(Currency.GBP);
    }

    @Test
    void shouldReturnSavedTransaction() {
        CreateTransactionCommand command =
                new CreateTransactionCommand(new BigDecimal("10.00"), Currency.GBP, TransactionType.DEPOSIT, null);

        Transaction persisted = new Transaction(account, command.amount(), command.type(), command.reference());
        when(accountRepository.findByAccountNumberForUpdate(ACCOUNT_NUMBER)).thenReturn(Optional.of(account));
        when(transactionRepository.save(any(Transaction.class))).thenReturn(persisted);

        Transaction result = transactionService.createTransaction(USER_ID, ACCOUNT_NUMBER, command);

        assertThat(result).isSameAs(persisted);
    }

    @Test
    void shouldDecreaseBalanceByWithdrawalAmount() {
        CreateTransactionCommand command =
                new CreateTransactionCommand(new BigDecimal("30.00"), Currency.GBP, TransactionType.WITHDRAWAL, null);

        when(accountRepository.findByAccountNumberForUpdate(ACCOUNT_NUMBER)).thenReturn(Optional.of(account));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> inv.getArgument(0));

        transactionService.createTransaction(USER_ID, ACCOUNT_NUMBER, command);

        assertThat(account.getBalance()).isEqualByComparingTo(new BigDecimal("70.00"));
    }

    @Test
    void shouldAllowWithdrawalEqualToBalance() {
        CreateTransactionCommand command =
                new CreateTransactionCommand(new BigDecimal("100.00"), Currency.GBP, TransactionType.WITHDRAWAL, null);

        when(accountRepository.findByAccountNumberForUpdate(ACCOUNT_NUMBER)).thenReturn(Optional.of(account));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> inv.getArgument(0));

        transactionService.createTransaction(USER_ID, ACCOUNT_NUMBER, command);

        assertThat(account.getBalance()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void shouldThrowUnprocessableEntityWhenInsufficientFunds() {
        CreateTransactionCommand command =
                new CreateTransactionCommand(new BigDecimal("100.01"), Currency.GBP, TransactionType.WITHDRAWAL, null);

        when(accountRepository.findByAccountNumberForUpdate(ACCOUNT_NUMBER)).thenReturn(Optional.of(account));

        assertThatThrownBy(() -> transactionService.createTransaction(USER_ID, ACCOUNT_NUMBER, command))
                .isInstanceOf(UnprocessableEntityException.class)
                .hasMessage("Insufficient funds to process transaction");

        verify(transactionRepository, never()).save(any());
    }

    @Test
    void shouldNotChangeBalanceWhenInsufficientFunds() {
        CreateTransactionCommand command =
                new CreateTransactionCommand(new BigDecimal("999.00"), Currency.GBP, TransactionType.WITHDRAWAL, null);

        when(accountRepository.findByAccountNumberForUpdate(ACCOUNT_NUMBER)).thenReturn(Optional.of(account));

        assertThatThrownBy(() -> transactionService.createTransaction(USER_ID, ACCOUNT_NUMBER, command))
                .isInstanceOf(UnprocessableEntityException.class);

        assertThat(account.getBalance()).isEqualByComparingTo(new BigDecimal("100.00"));
    }

    @Test
    void shouldThrowForbiddenWhenAccountBelongsToAnotherUser() {
        User otherOwner = new User();
        otherOwner.setId(OTHER_USER_ID);
        account.setUser(otherOwner);

        CreateTransactionCommand command =
                new CreateTransactionCommand(new BigDecimal("50.00"), Currency.GBP, TransactionType.DEPOSIT, null);

        when(accountRepository.findByAccountNumberForUpdate(ACCOUNT_NUMBER)).thenReturn(Optional.of(account));

        assertThatThrownBy(() -> transactionService.createTransaction(USER_ID, ACCOUNT_NUMBER, command))
                .isInstanceOf(ForbiddenException.class)
                .hasMessage("You are not allowed to access this bank account");

        verify(transactionRepository, never()).save(any());
    }

    @Test
    void shouldThrowNotFoundWhenTryingToFindUnexistingAccount() {
        CreateTransactionCommand command =
                new CreateTransactionCommand(new BigDecimal("50.00"), Currency.GBP, TransactionType.DEPOSIT, null);

        when(accountRepository.findByAccountNumberForUpdate(ACCOUNT_NUMBER)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> transactionService.createTransaction(USER_ID, ACCOUNT_NUMBER, command))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Bank account was not found");

        verify(transactionRepository, never()).save(any());
    }
}
