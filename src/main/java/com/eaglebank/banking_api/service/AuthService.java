package com.eaglebank.banking_api.service;

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
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtService jwtService;
    private final TokenHasher tokenHasher;
    private final Duration refreshTokenExpiry;

    public AuthService(
            UserRepository userRepository,
            RefreshTokenRepository refreshTokenRepository,
            JwtService jwtService,
            TokenHasher tokenHasher,
            @Value("${security.jwt.refresh-token-expiry}") Duration refreshTokenExpiry) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.jwtService = jwtService;
        this.tokenHasher = tokenHasher;
        this.refreshTokenExpiry = refreshTokenExpiry;
    }

    @Transactional
    public AuthResult login(String email) {
        User user =
                userRepository.findByEmail(email).orElseThrow(() -> new InvalidCredentialsException("Invalid email"));

        log.info("Login successful for user {}", user.getId());

        int revokedCount = refreshTokenRepository.revokeAllByUserId(user.getId());
        if (revokedCount > 0) {
            log.info("Revoked {} existing refresh tokens for user {}", revokedCount, user.getId());
        }

        log.info("Login successful for user {}", user.getId());
        return issueTokens(user);
    }

    @Transactional
    public AuthResult refresh(String rawRefreshToken) {
        String tokenHash = tokenHasher.hash(rawRefreshToken);

        RefreshToken persistedRefreshToken = refreshTokenRepository
                .findByTokenHash(tokenHash)
                .orElseThrow(() -> new InvalidTokenException("Refresh token is invalid"));

        if (persistedRefreshToken.isRevoked()) {
            log.info(
                    "Refresh token is already revoked. Revoking all sessions for user {}",
                    persistedRefreshToken.getUser().getId());
            refreshTokenRepository.revokeAllByUserId(
                    persistedRefreshToken.getUser().getId());
            throw new InvalidTokenException("Refresh token has been revoked");
        }

        if (persistedRefreshToken.isExpired()) {
            throw new InvalidTokenException("Refresh token is expired");
        }

        User user = persistedRefreshToken.getUser();

        persistedRefreshToken.setRevoked(true);
        refreshTokenRepository.save(persistedRefreshToken);

        return issueTokens(user);
    }

    private AuthResult issueTokens(User user) {
        String accessToken = jwtService.generateAccessToken(user.getId(), user.getEmail());

        String rawRefreshToken = UUID.randomUUID().toString();
        String tokenHash = tokenHasher.hash(rawRefreshToken);

        RefreshToken refreshToken =
                new RefreshToken(tokenHash, user, LocalDateTime.now().plus(refreshTokenExpiry));
        refreshTokenRepository.save(refreshToken);

        return new AuthResult(accessToken, rawRefreshToken, jwtService.getAccessTokenExpirySeconds());
    }
}
