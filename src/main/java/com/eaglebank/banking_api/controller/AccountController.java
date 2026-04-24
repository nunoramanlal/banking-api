package com.eaglebank.banking_api.controller;

import com.eaglebank.banking_api.dto.request.CreateBankAccountRequest;
import com.eaglebank.banking_api.dto.response.BankAccountResponse;
import com.eaglebank.banking_api.dto.response.BadRequestErrorResponse;
import com.eaglebank.banking_api.dto.response.ErrorResponse;
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
}
