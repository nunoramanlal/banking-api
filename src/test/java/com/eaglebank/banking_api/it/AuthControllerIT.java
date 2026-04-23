package com.eaglebank.banking_api.it;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.eaglebank.banking_api.dto.AddressDto;
import com.eaglebank.banking_api.dto.request.CreateUserRequest;
import com.eaglebank.banking_api.dto.request.LoginRequest;
import com.eaglebank.banking_api.dto.request.RefreshTokenRequest;
import com.eaglebank.banking_api.repository.RefreshTokenRepository;
import com.eaglebank.banking_api.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @AfterEach
    void cleanUp() {
        refreshTokenRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Nested
    class Login {

        @Test
        void shouldReturnAccessAndRefreshTokensWhenEmailExists() throws Exception {
            createUser("test@example.com");

            LoginRequest request = new LoginRequest("test@example.com");

            mockMvc.perform(post("/v1/auth/login")
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
            createUser("test@example.com");

            String firstRefreshToken = loginAndGetRefreshToken("test@example.com");
            String secondRefreshToken = loginAndGetRefreshToken("test@example.com");

            assertThat(firstRefreshToken).isNotEqualTo(secondRefreshToken);

            // Old refresh token should no longer work
            mockMvc.perform(post("/v1/auth/refresh")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new RefreshTokenRequest(firstRefreshToken))))
                    .andExpect(status().isUnauthorized());

            // New refresh token should work
            mockMvc.perform(post("/v1/auth/refresh")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new RefreshTokenRequest(secondRefreshToken))))
                    .andExpect(status().isOk());
        }

        @Test
        void shouldReturnUnauthorizedWhenEmailDoesNotExist() throws Exception {
            LoginRequest request = new LoginRequest("nonexistent@example.com");

            mockMvc.perform(post("/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.message").exists());

            assertThat(refreshTokenRepository.findAll()).isEmpty();
        }

        @Test
        void shouldReturnBadRequestWhenEmailIsMissing() throws Exception {
            mockMvc.perform(post("/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.details[0].field").value("email"));
        }

        @Test
        void shouldReturnBadRequestWhenEmailIsInvalidFormat() throws Exception {
            LoginRequest request = new LoginRequest("not-an-email");

            mockMvc.perform(post("/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.details[0].field").value("email"));
        }

        @Test
        void shouldReturnBadRequestWhenRequestBodyIsEmpty() throws Exception {
            mockMvc.perform(post("/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(""))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    class Refresh {

        @Test
        void shouldReturnNewTokensWhenRefreshTokenIsValid() throws Exception {
            createUser("test@example.com");
            String refreshToken = loginAndGetRefreshToken("test@example.com");

            RefreshTokenRequest request = new RefreshTokenRequest(refreshToken);

            mockMvc.perform(post("/v1/auth/refresh")
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
            createUser("test@example.com");
            String oldRefreshToken = loginAndGetRefreshToken("test@example.com");

            // First refresh - succeeds
            mockMvc.perform(post("/v1/auth/refresh")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new RefreshTokenRequest(oldRefreshToken))))
                    .andExpect(status().isOk());

            // Try to reuse the old token - should fail
            mockMvc.perform(post("/v1/auth/refresh")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new RefreshTokenRequest(oldRefreshToken))))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.message").exists());
        }

        @Test
        void shouldReturnUnauthorizedWhenRefreshTokenDoesNotExist() throws Exception {
            RefreshTokenRequest request = new RefreshTokenRequest("nonexistent-token");

            mockMvc.perform(post("/v1/auth/refresh")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.message").exists());
        }

        @Test
        void shouldReturnBadRequestWhenRefreshTokenIsBlank() throws Exception {
            RefreshTokenRequest request = new RefreshTokenRequest("");

            mockMvc.perform(post("/v1/auth/refresh")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.details[0].field").value("refreshToken"));
        }
    }

    private void createUser(String email) throws Exception {
        CreateUserRequest request = new CreateUserRequest(
                "test-name",
                new AddressDto("test-line1", null, null, "test-town", "test-county", "TEST 123"),
                "+447911123456",
                email);

        mockMvc.perform(post("/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }

    private String loginAndGetRefreshToken(String email) throws Exception {
        MvcResult result = mockMvc.perform(post("/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest(email))))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
        return response.get("refreshToken").asText();
    }
}
