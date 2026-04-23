package com.eaglebank.banking_api.mapper.auth;

import com.eaglebank.banking_api.dto.response.LoginResponse;
import com.eaglebank.banking_api.service.result.AuthResult;
import org.springframework.stereotype.Component;

@Component
public class AuthResponseMapper {

    private static final String TOKEN_TYPE = "Bearer";

    public LoginResponse toLoginResponse(AuthResult result) {
        return new LoginResponse(result.accessToken(), result.refreshToken(), TOKEN_TYPE, result.expiresInSeconds());
    }
}
