package com.eaglebank.banking_api.mapper.auth;

import static org.assertj.core.api.Assertions.assertThat;

import com.eaglebank.banking_api.dto.response.LoginResponse;
import com.eaglebank.banking_api.service.result.AuthResult;
import org.junit.jupiter.api.Test;

class AuthResponseMapperTest {

    private final AuthResponseMapper mapper = new AuthResponseMapper();

    @Test
    void shouldMapAllFieldsFromAuthResultToLoginResponse() {
        AuthResult result = new AuthResult("access-token-value", "refresh-token-value", 900L);

        LoginResponse response = mapper.toLoginResponse(result);

        assertThat(response.accessToken()).isEqualTo("access-token-value");
        assertThat(response.refreshToken()).isEqualTo("refresh-token-value");
        assertThat(response.expiresIn()).isEqualTo(900L);
    }

    @Test
    void shouldSetTokenTypeToBearer() {
        AuthResult result = new AuthResult("access", "refresh", 900L);

        LoginResponse response = mapper.toLoginResponse(result);

        assertThat(response.tokenType()).isEqualTo("Bearer");
    }
}
