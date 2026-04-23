package com.eaglebank.banking_api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import com.eaglebank.banking_api.entity.RefreshToken;
import com.eaglebank.banking_api.entity.User;
import com.eaglebank.banking_api.exception.InvalidCredentialsException;
import com.eaglebank.banking_api.exception.InvalidTokenException;
import com.eaglebank.banking_api.repository.RefreshTokenRepository;
import com.eaglebank.banking_api.repository.UserRepository;
import com.eaglebank.banking_api.security.JwtService;
import com.eaglebank.banking_api.service.result.AuthResult;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private JwtService jwtService;

    private AuthService authService;

    private static final Duration REFRESH_TOKEN_EXPIRY = Duration.ofDays(7);
    private static final String USER_ID = "usr-test123";
    private static final String EMAIL = "test@example.com";
    private static final String ACCESS_TOKEN = "access-token-value";

    @BeforeEach
    void setUp() {
        authService = new AuthService(userRepository, refreshTokenRepository, jwtService, REFRESH_TOKEN_EXPIRY);
    }

    @Nested
    class Login {

        @Test
        void shouldIssueTokensWhenUserExists() {
            User user = buildUser();
            when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
            when(jwtService.generateAccessToken(USER_ID, EMAIL)).thenReturn(ACCESS_TOKEN);
            when(jwtService.getAccessTokenExpirySeconds()).thenReturn(900L);

            AuthResult result = authService.login(EMAIL);

            assertThat(result.accessToken()).isEqualTo(ACCESS_TOKEN);
            assertThat(result.refreshToken()).isNotBlank();
            assertThat(result.expiresInSeconds()).isEqualTo(900L);
        }

        @Test
        void shouldPersistRefreshTokenOnLogin() {
            User user = buildUser();
            when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
            when(jwtService.generateAccessToken(anyString(), anyString())).thenReturn(ACCESS_TOKEN);

            authService.login(EMAIL);

            ArgumentCaptor<RefreshToken> tokenCaptor = ArgumentCaptor.forClass(RefreshToken.class);
            verify(refreshTokenRepository).save(tokenCaptor.capture());

            RefreshToken saved = tokenCaptor.getValue();
            assertThat(saved.getUserId()).isEqualTo(USER_ID);
            assertThat(saved.getToken()).isNotBlank();
            assertThat(saved.isRevoked()).isFalse();
            assertThat(saved.getExpiresAt()).isAfter(LocalDateTime.now().plusDays(6));
        }

        @Test
        void shouldRevokeExistingTokensBeforeIssuingNew() {
            User user = buildUser();
            when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
            when(refreshTokenRepository.revokeAllByUserId(USER_ID)).thenReturn(2);
            when(jwtService.generateAccessToken(anyString(), anyString())).thenReturn(ACCESS_TOKEN);

            authService.login(EMAIL);

            verify(refreshTokenRepository).revokeAllByUserId(USER_ID);
        }

        @Test
        void shouldThrowInvalidCredentialsWhenUserDoesNotExist() {
            when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> authService.login(EMAIL))
                    .isInstanceOf(InvalidCredentialsException.class)
                    .hasMessageContaining("Invalid email");

            verify(refreshTokenRepository, never()).save(any());
            verify(jwtService, never()).generateAccessToken(anyString(), anyString());
        }
    }

    @Nested
    class Refresh {

        @Test
        void shouldIssueNewTokensWhenRefreshTokenIsValid() {
            User user = buildUser();
            RefreshToken validToken = buildValidRefreshToken();
            when(refreshTokenRepository.findByToken("valid-token")).thenReturn(Optional.of(validToken));
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
            when(jwtService.generateAccessToken(USER_ID, EMAIL)).thenReturn(ACCESS_TOKEN);
            when(jwtService.getAccessTokenExpirySeconds()).thenReturn(900L);

            AuthResult result = authService.refresh("valid-token");

            assertThat(result.accessToken()).isEqualTo(ACCESS_TOKEN);
            assertThat(result.refreshToken()).isNotBlank().isNotEqualTo("valid-token");
        }

        @Test
        void shouldRevokeOldRefreshTokenOnRotation() {
            User user = buildUser();
            RefreshToken validToken = buildValidRefreshToken();
            when(refreshTokenRepository.findByToken("valid-token")).thenReturn(Optional.of(validToken));
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
            when(jwtService.generateAccessToken(anyString(), anyString())).thenReturn(ACCESS_TOKEN);

            authService.refresh("valid-token");

            assertThat(validToken.isRevoked()).isTrue();
            verify(refreshTokenRepository).save(validToken);
        }

        @Test
        void shouldThrowInvalidTokenWhenTokenNotFound() {
            when(refreshTokenRepository.findByToken("nonexistent")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> authService.refresh("nonexistent"))
                    .isInstanceOf(InvalidTokenException.class)
                    .hasMessageContaining("invalid");
        }

        @Test
        void shouldThrowInvalidTokenWhenTokenIsRevoked() {
            RefreshToken revokedToken = buildValidRefreshToken();
            revokedToken.setRevoked(true);
            when(refreshTokenRepository.findByToken("revoked")).thenReturn(Optional.of(revokedToken));

            assertThatThrownBy(() -> authService.refresh("revoked"))
                    .isInstanceOf(InvalidTokenException.class)
                    .hasMessageContaining("expired or revoked");

            verify(jwtService, never()).generateAccessToken(anyString(), anyString());
        }

        @Test
        void shouldThrowInvalidTokenWhenTokenIsExpired() {
            RefreshToken expiredToken =
                    new RefreshToken("expired", USER_ID, LocalDateTime.now().minusHours(1));
            when(refreshTokenRepository.findByToken("expired")).thenReturn(Optional.of(expiredToken));

            assertThatThrownBy(() -> authService.refresh("expired"))
                    .isInstanceOf(InvalidTokenException.class)
                    .hasMessageContaining("expired or revoked");

            verify(jwtService, never()).generateAccessToken(anyString(), anyString());
        }

        @Test
        void shouldThrowInvalidTokenWhenUserNotFound() {
            RefreshToken validToken = buildValidRefreshToken();
            when(refreshTokenRepository.findByToken("valid-token")).thenReturn(Optional.of(validToken));
            when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> authService.refresh("valid-token"))
                    .isInstanceOf(InvalidTokenException.class)
                    .hasMessageContaining("User not found");
        }
    }

    private User buildUser() {
        User user = new User(
                "test-name",
                EMAIL,
                "+447911123456",
                "test-line1",
                "test-line2",
                "test-line3",
                "test-town",
                "test-county",
                "TEST 123");
        user.setId(USER_ID);
        return user;
    }

    private RefreshToken buildValidRefreshToken() {
        return new RefreshToken("valid-token", USER_ID, LocalDateTime.now().plusDays(7));
    }
}
