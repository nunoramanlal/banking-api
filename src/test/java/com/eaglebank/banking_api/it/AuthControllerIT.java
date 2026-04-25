package com.eaglebank.banking_api.it;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.eaglebank.banking_api.dto.request.LoginRequest;
import com.eaglebank.banking_api.dto.request.RefreshTokenRequest;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

class AuthControllerIT extends IntegrationUtils {

    private static final String REFRESH_ENDPOINT = "/v1/auth/refresh";

    @Nested
    class Login {

        @Test
        void shouldReturnAccessAndRefreshTokensWhenEmailExists() throws Exception {
            createUserAndGetId(TEST_EMAIL);

            LoginRequest request = new LoginRequest(TEST_EMAIL);

            mockMvc.perform(post(LOGIN_ENDPOINT)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.accessToken").isNotEmpty())
                    .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                    .andExpect(jsonPath("$.tokenType").value("Bearer"))
                    .andExpect(jsonPath("$.expiresIn").value(900));

            assertThat(refreshTokenRepository.findAll()).hasSize(1);
        }

        @Test
        void shouldRevokePreviousRefreshTokenWhenLoggingInTwice() throws Exception {
            createUserAndGetId(TEST_EMAIL);

            String firstRefreshToken = loginAndGetRefreshToken(TEST_EMAIL);
            String secondRefreshToken = loginAndGetRefreshToken(TEST_EMAIL);

            assertThat(firstRefreshToken).isNotEqualTo(secondRefreshToken);

            // Old refresh token should no longer work
            mockMvc.perform(post(REFRESH_ENDPOINT)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new RefreshTokenRequest(firstRefreshToken))))
                    .andExpect(status().isUnauthorized());

            // New refresh token should work
            mockMvc.perform(post(REFRESH_ENDPOINT)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new RefreshTokenRequest(secondRefreshToken))))
                    .andExpect(status().isOk());
        }

        @Test
        void shouldReturnUnauthorizedWhenEmailDoesNotExist() throws Exception {
            LoginRequest request = new LoginRequest("nonexistent@example.com");

            mockMvc.perform(post(LOGIN_ENDPOINT)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.message").exists());

            assertThat(refreshTokenRepository.findAll()).isEmpty();
        }

        @Test
        void shouldReturnBadRequestWhenEmailIsMissing() throws Exception {
            mockMvc.perform(post(LOGIN_ENDPOINT)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.details[0].field").value("email"));
        }

        @Test
        void shouldReturnBadRequestWhenEmailIsInvalidFormat() throws Exception {
            LoginRequest request = new LoginRequest("not-an-email");

            mockMvc.perform(post(LOGIN_ENDPOINT)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.details[0].field").value("email"));
        }

        @Test
        void shouldReturnBadRequestWhenRequestBodyIsEmpty() throws Exception {
            mockMvc.perform(post(LOGIN_ENDPOINT)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(""))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    class Refresh {

        @Test
        void shouldReturnNewTokensWhenRefreshTokenIsValid() throws Exception {
            createUserAndGetId(TEST_EMAIL);
            String refreshToken = loginAndGetRefreshToken(TEST_EMAIL);

            RefreshTokenRequest request = new RefreshTokenRequest(refreshToken);

            mockMvc.perform(post(REFRESH_ENDPOINT)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.accessToken").isNotEmpty())
                    .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                    .andExpect(jsonPath("$.refreshToken").value(org.hamcrest.Matchers.not(refreshToken)))
                    .andExpect(jsonPath("$.tokenType").value("Bearer"));
        }

        @Test
        void shouldRevokeOldRefreshTokenAfterRotation() throws Exception {
            createUserAndGetId(TEST_EMAIL);
            String oldRefreshToken = loginAndGetRefreshToken(TEST_EMAIL);

            // First refresh - succeeds
            mockMvc.perform(post(REFRESH_ENDPOINT)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new RefreshTokenRequest(oldRefreshToken))))
                    .andExpect(status().isOk());

            // Try to reuse the old token - should fail
            mockMvc.perform(post(REFRESH_ENDPOINT)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new RefreshTokenRequest(oldRefreshToken))))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.message").exists());
        }

        @Test
        void shouldReturnUnauthorizedWhenRefreshTokenDoesNotExist() throws Exception {
            RefreshTokenRequest request = new RefreshTokenRequest("nonexistent-token");

            mockMvc.perform(post(REFRESH_ENDPOINT)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.message").exists());
        }

        @Test
        void shouldReturnBadRequestWhenRefreshTokenIsBlank() throws Exception {
            RefreshTokenRequest request = new RefreshTokenRequest("");

            mockMvc.perform(post(REFRESH_ENDPOINT)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.details[0].field").value("refreshToken"));
        }
    }
}
