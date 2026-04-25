package com.eaglebank.banking_api.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(name = "ListBankAccountsResponse", description = "List of bank accounts for the authenticated user")
public record ListBankAccountsResponse(
        @Schema(
                description = "Bank accounts owned by the authenticated user",
                requiredMode = Schema.RequiredMode.REQUIRED)
        List<BankAccountResponse> accounts) {}
