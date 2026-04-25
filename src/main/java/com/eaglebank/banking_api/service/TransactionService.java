package com.eaglebank.banking_api.service;

import com.eaglebank.banking_api.entity.Account;
import com.eaglebank.banking_api.entity.Transaction;
import com.eaglebank.banking_api.entity.enums.TransactionType;
import com.eaglebank.banking_api.exception.ForbiddenException;
import com.eaglebank.banking_api.exception.NotFoundException;
import com.eaglebank.banking_api.exception.UnprocessableEntityException;
import com.eaglebank.banking_api.repository.AccountRepository;
import com.eaglebank.banking_api.repository.TransactionRepository;
import com.eaglebank.banking_api.service.command.CreateTransactionCommand;
import java.math.BigDecimal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;

    public TransactionService(TransactionRepository transactionRepository, AccountRepository accountRepository) {
        this.transactionRepository = transactionRepository;
        this.accountRepository = accountRepository;
    }

    @Transactional
    public Transaction createTransaction(String userId, Long accountNumber, CreateTransactionCommand command) {
        log.debug(
                "Processing {} of {} on account {} for user {}",
                command.type(),
                command.amount(),
                accountNumber,
                userId);

        // Lock the account row for the duration of this transaction.
        Account account = accountRepository
                .findByAccountNumberForUpdate(accountNumber)
                .orElseThrow(() -> new NotFoundException("Bank account was not found"));

        if (!account.getUser().getId().equals(userId)) {
            throw new ForbiddenException("You are not allowed to access this bank account");
        }

        if (account.getCurrency() != command.currency()) {
            throw new UnprocessableEntityException("Transaction currency does not match account currency");
        }

        BigDecimal balanceBefore = account.getBalance();
        BigDecimal balanceAfter = applyTransaction(balanceBefore, command.amount(), command.type());
        account.setBalance(balanceAfter);

        Transaction transaction = new Transaction(account, command.amount(), command.type(), command.reference());

        return transactionRepository.save(transaction);
    }

    private BigDecimal applyTransaction(BigDecimal currentBalance, BigDecimal amount, TransactionType type) {
        return switch (type) {
            case DEPOSIT -> currentBalance.add(amount);
            case WITHDRAWAL -> {
                BigDecimal result = currentBalance.subtract(amount);
                if (result.compareTo(BigDecimal.ZERO) < 0) {
                    throw new UnprocessableEntityException("Insufficient funds to process transaction");
                }
                yield result;
            }
        };
    }
}
