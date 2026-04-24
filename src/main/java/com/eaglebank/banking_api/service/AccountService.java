package com.eaglebank.banking_api.service;

import com.eaglebank.banking_api.entity.Account;
import com.eaglebank.banking_api.entity.User;
import com.eaglebank.banking_api.exception.ForbiddenException;
import com.eaglebank.banking_api.exception.NotFoundException;
import com.eaglebank.banking_api.repository.AccountRepository;
import com.eaglebank.banking_api.repository.UserRepository;
import com.eaglebank.banking_api.service.command.CreateAccountCommand;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class AccountService {
    private final AccountRepository accountRepository;
    private final UserRepository userRepository;

    public AccountService(AccountRepository accountRepository, UserRepository userRepository) {
        this.accountRepository = accountRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    @PreAuthorize("#userId == authentication.principal")
    public Account createBankAccount(String userId, CreateAccountCommand command) {
        log.info("Creating bank account for user: {}", userId);

        User user = userRepository.findById(userId).orElseThrow(() -> new NotFoundException("User was not found"));

        Account account = new Account(command.name(), command.accountType(), user);
        Account savedAccount = accountRepository.save(account);

        log.info("Bank account created successfully with account number: {}", savedAccount.getAccountNumber());
        return savedAccount;
    }

    @Transactional(readOnly = true)
    public List<Account> listAccounts(String userId) {
        log.info("Listing bank accounts for user: {}", userId);
        return accountRepository.findByUserId(userId);
    }

    @Transactional(readOnly = true)
    public Account fetchAccountByAccountNumber(String userId, Long accountNumber) {
        log.info("Fetching bank account: {} for user: {}", accountNumber, userId);

        Account account = accountRepository
                .findByAccountNumber(accountNumber)
                .orElseThrow(() -> new NotFoundException("Bank account was not found"));

        if (!account.getUser().getId().equals(userId)) {
            throw new ForbiddenException("You are not allowed to access this bank account");
        }

        return account;
    }
}
