package com.eaglebank.banking_api.dto.response;

public record ValidationError(String field, String message, ValidationErrorType type) {}
