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
import com.eaglebank.banking_api.security.TokenHasher;
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

    private final TokenHasher tokenHasher = new TokenHasher();

    private AuthService authService;

    private static final Duration REFRESH_TOKEN_EXPIRY = Duration.ofDays(7);
    private static final String USER_ID = "usr-test123";
    private static final String EMAIL = "test@example.com";
    private static final String ACCESS_TOKEN = "access-token-value";

    @BeforeEach
    void setUp() {
        authService =
                new AuthService(userRepository, refreshTokenRepository, jwtService, tokenHasher, REFRESH_TOKEN_EXPIRY);
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
        void shouldPersistHashedRefreshTokenOnLogin() {
            User user = buildUser();
            when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
            when(jwtService.generateAccessToken(anyString(), anyString())).thenReturn(ACCESS_TOKEN);

            AuthResult result = authService.login(EMAIL);

            ArgumentCaptor<RefreshToken> tokenCaptor = ArgumentCaptor.forClass(RefreshToken.class);
            verify(refreshTokenRepository).save(tokenCaptor.capture());

            RefreshToken saved = tokenCaptor.getValue();
            assertThat(saved.getUser()).isEqualTo(user);
            assertThat(saved.isRevoked()).isFalse();
            assertThat(saved.getExpiresAt()).isAfter(LocalDateTime.now().plusDays(6));

            assertThat(saved.getTokenHash())
                    .isEqualTo(tokenHasher.hash(result.refreshToken()))
                    .hasSize(64)
                    .isNotEqualTo(result.refreshToken());
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
            String rawToken = "valid-token";
            RefreshToken validToken = buildValidRefreshToken(user, rawToken);
            when(refreshTokenRepository.findByTokenHash(tokenHasher.hash(rawToken)))
                    .thenReturn(Optional.of(validToken));
            when(jwtService.generateAccessToken(USER_ID, EMAIL)).thenReturn(ACCESS_TOKEN);
            when(jwtService.getAccessTokenExpirySeconds()).thenReturn(900L);

            AuthResult result = authService.refresh(rawToken);

            assertThat(result.accessToken()).isEqualTo(ACCESS_TOKEN);
            assertThat(result.refreshToken()).isNotBlank().isNotEqualTo(rawToken);
        }

        @Test
        void shouldRevokeOldRefreshTokenOnRotation() {
            User user = buildUser();
            String rawToken = "valid-token";
            RefreshToken validToken = buildValidRefreshToken(user, rawToken);
            when(refreshTokenRepository.findByTokenHash(tokenHasher.hash(rawToken)))
                    .thenReturn(Optional.of(validToken));
            when(jwtService.generateAccessToken(anyString(), anyString())).thenReturn(ACCESS_TOKEN);

            authService.refresh(rawToken);

            assertThat(validToken.isRevoked()).isTrue();
            verify(refreshTokenRepository).save(validToken);
        }

        @Test
        void shouldThrowInvalidTokenWhenTokenNotFound() {
            when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.empty());

            assertThatThrownBy(() -> authService.refresh("nonexistent"))
                    .isInstanceOf(InvalidTokenException.class)
                    .hasMessageContaining("invalid");
        }

        @Test
        void shouldRevokeAllSessionsWhenRevokedTokenIsPresented() {
            User user = buildUser();
            String rawToken = "stolen-token";
            RefreshToken revokedToken = buildValidRefreshToken(user, rawToken);
            revokedToken.setRevoked(true);
            when(refreshTokenRepository.findByTokenHash(tokenHasher.hash(rawToken)))
                    .thenReturn(Optional.of(revokedToken));

            assertThatThrownBy(() -> authService.refresh(rawToken))
                    .isInstanceOf(InvalidTokenException.class)
                    .hasMessageContaining("revoked");

            verify(refreshTokenRepository).revokeAllByUserId(USER_ID);
            verify(jwtService, never()).generateAccessToken(anyString(), anyString());
        }

        @Test
        void shouldThrowInvalidTokenWhenTokenIsExpired() {
            User user = buildUser();
            String rawToken = "expired";
            RefreshToken expiredToken = new RefreshToken(
                    tokenHasher.hash(rawToken), user, LocalDateTime.now().minusHours(1));
            when(refreshTokenRepository.findByTokenHash(tokenHasher.hash(rawToken)))
                    .thenReturn(Optional.of(expiredToken));

            assertThatThrownBy(() -> authService.refresh(rawToken))
                    .isInstanceOf(InvalidTokenException.class)
                    .hasMessageContaining("expired");

            verify(jwtService, never()).generateAccessToken(anyString(), anyString());
            verify(refreshTokenRepository, never()).revokeAllByUserId(anyString());
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

    private RefreshToken buildValidRefreshToken(User user, String rawToken) {
        return new RefreshToken(
                tokenHasher.hash(rawToken), user, LocalDateTime.now().plusDays(7));
    }
}
