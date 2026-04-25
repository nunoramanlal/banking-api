package com.eaglebank.banking_api.controller;

import com.eaglebank.banking_api.dto.request.CreateTransactionRequest;
import com.eaglebank.banking_api.dto.response.BadRequestErrorResponse;
import com.eaglebank.banking_api.dto.response.ErrorResponse;
import com.eaglebank.banking_api.dto.response.TransactionResponse;
import com.eaglebank.banking_api.entity.Transaction;
import com.eaglebank.banking_api.mapper.transaction.TransactionResponseMapper;
import com.eaglebank.banking_api.service.TransactionService;
import com.eaglebank.banking_api.service.command.CreateTransactionCommand;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/accounts/{accountNumber}/transactions")
@Validated
@Tag(name = "transaction", description = "Manage transactions on a bank account")
public class TransactionController {

    private final TransactionService transactionService;
    private final TransactionResponseMapper transactionResponseMapper;

    public TransactionController(
            TransactionService transactionService, TransactionResponseMapper transactionResponseMapper) {
        this.transactionService = transactionService;
        this.transactionResponseMapper = transactionResponseMapper;
    }

    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping
    @Operation(summary = "Create a transaction", description = "Deposit or withdraw money on a bank account")
    @ApiResponses(
            value = {
                @ApiResponse(
                        responseCode = "201",
                        description = "Transaction has been created successfully",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        schema = @Schema(implementation = TransactionResponse.class))),
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
                        description = "The user is not allowed to access the bank account",
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
                        responseCode = "422",
                        description = "Insufficient funds to process transaction",
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
    public TransactionResponse createTransaction(
            @PathVariable @Pattern(regexp = "^01\\d{6}$", message = "Account number format is invalid")
                    String accountNumber,
            @Valid @RequestBody CreateTransactionRequest request,
            Authentication authentication) {
        String userId = (String) authentication.getPrincipal();
        CreateTransactionCommand command =
                new CreateTransactionCommand(request.amount(), request.currency(), request.type(), request.reference());
        Transaction transaction = transactionService.createTransaction(userId, Long.valueOf(accountNumber), command);
        return transactionResponseMapper.toResponse(transaction, userId);
    }
}
