package com.eaglebank.banking_api.controller;

import com.eaglebank.banking_api.dto.request.CreateBankAccountRequest;
import com.eaglebank.banking_api.dto.response.BadRequestErrorResponse;
import com.eaglebank.banking_api.dto.response.BankAccountResponse;
import com.eaglebank.banking_api.dto.response.ErrorResponse;
import com.eaglebank.banking_api.dto.response.ListBankAccountsResponse;
import com.eaglebank.banking_api.entity.Account;
import com.eaglebank.banking_api.mapper.account.AccountResponseMapper;
import com.eaglebank.banking_api.service.AccountService;
import com.eaglebank.banking_api.service.command.CreateAccountCommand;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/accounts")
@Validated
@Tag(name = "account", description = "Manage a bank account")
public class AccountController {
    private final AccountService accountService;
    private final AccountResponseMapper bankAccountResponseMapper;

    public AccountController(AccountService accountService, AccountResponseMapper bankAccountResponseMapper) {
        this.accountService = accountService;
        this.bankAccountResponseMapper = bankAccountResponseMapper;
    }

    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping
    @Operation(summary = "Create a new bank account", description = "Create a new bank account for the user")
    @ApiResponses(
            value = {
                @ApiResponse(
                        responseCode = "201",
                        description = "Bank Account has been created successfully",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        schema = @Schema(implementation = BankAccountResponse.class))),
                @ApiResponse(
                        responseCode = "400",
                        description = "Invalid details supplied",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        schema = @Schema(implementation = BadRequestErrorResponse.class))),
                @ApiResponse(
                        responseCode = "401",
                        description = "Access token is missing or invalid",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        schema = @Schema(implementation = ErrorResponse.class))),
                @ApiResponse(
                        responseCode = "403",
                        description = "The user is not allowed to access the transaction",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        schema = @Schema(implementation = ErrorResponse.class))),
                @ApiResponse(
                        responseCode = "500",
                        description = "An unexpected error occurred",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        schema = @Schema(implementation = ErrorResponse.class)))
            })
    public BankAccountResponse createAccount(
            @Valid @RequestBody CreateBankAccountRequest request, Authentication authentication) {
        String userId = (String) authentication.getPrincipal();
        CreateAccountCommand command = new CreateAccountCommand(request.name(), request.accountType());
        Account account = accountService.createBankAccount(userId, command);
        return bankAccountResponseMapper.toResponse(account);
    }

    @ResponseStatus(HttpStatus.OK)
    @GetMapping
    @Operation(summary = "List accounts", description = "List all bank accounts for the authenticated user")
    @ApiResponses(
            value = {
                @ApiResponse(
                        responseCode = "200",
                        description = "The list of bank accounts",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        schema = @Schema(implementation = ListBankAccountsResponse.class))),
                @ApiResponse(
                        responseCode = "401",
                        description = "Access token is missing or invalid",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        schema = @Schema(implementation = ErrorResponse.class))),
                @ApiResponse(
                        responseCode = "500",
                        description = "An unexpected error occurred",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        schema = @Schema(implementation = ErrorResponse.class)))
            })
    public ListBankAccountsResponse listAccounts(Authentication authentication) {
        String userId = (String) authentication.getPrincipal();
        List<Account> accounts = accountService.listAccounts(userId);
        List<BankAccountResponse> responses =
                accounts.stream().map(bankAccountResponseMapper::toResponse).toList();
        return new ListBankAccountsResponse(responses);
    }

    @ResponseStatus(HttpStatus.OK)
    @GetMapping("/{accountNumber}")
    @Operation(summary = "Fetch account by account number", description = "Fetch the details of a bank account")
    @ApiResponses(
            value = {
                @ApiResponse(
                        responseCode = "200",
                        description = "The bank account details",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        schema = @Schema(implementation = BankAccountResponse.class))),
                @ApiResponse(
                        responseCode = "400",
                        description = "The request didn't supply all the necessary data",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        schema = @Schema(implementation = BadRequestErrorResponse.class))),
                @ApiResponse(
                        responseCode = "401",
                        description = "Access token is missing or invalid",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        schema = @Schema(implementation = ErrorResponse.class))),
                @ApiResponse(
                        responseCode = "403",
                        description = "The user is not allowed to access the bank account details",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        schema = @Schema(implementation = ErrorResponse.class))),
                @ApiResponse(
                        responseCode = "404",
                        description = "Bank account was not found",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        schema = @Schema(implementation = ErrorResponse.class))),
                @ApiResponse(
                        responseCode = "500",
                        description = "An unexpected error occurred",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        schema = @Schema(implementation = ErrorResponse.class)))
            })
    public BankAccountResponse fetchAccountByAccountNumber(
            @PathVariable @Pattern(regexp = "^01\\d{6}$", message = "Account number format is invalid")
                    String accountNumber,
            Authentication authentication) {
        String userId = (String) authentication.getPrincipal();
        Account account = accountService.fetchAccountByAccountNumber(userId, Long.valueOf(accountNumber));
        return bankAccountResponseMapper.toResponse(account);
    }
}
