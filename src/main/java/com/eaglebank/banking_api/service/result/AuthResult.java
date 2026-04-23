package com.eaglebank.banking_api.service.result;

public record AuthResult(String accessToken, String refreshToken, long expiresInSeconds) {}
