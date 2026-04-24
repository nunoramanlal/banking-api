package com.eaglebank.banking_api.service;

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
    private final Duration refreshTokenExpiry;

    public AuthService(
            UserRepository userRepository,
            RefreshTokenRepository refreshTokenRepository,
            JwtService jwtService,
            @Value("${security.jwt.refresh-token-expiry}") Duration refreshTokenExpiry) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.jwtService = jwtService;
        this.refreshTokenExpiry = refreshTokenExpiry;
    }

    @Transactional
    public AuthResult login(String email) {
        log.info("Login attempt for email: {}", email);

        User user =
                userRepository.findByEmail(email).orElseThrow(() -> new InvalidCredentialsException("Invalid email"));

        int revokedCount = refreshTokenRepository.revokeAllByUserId(user.getId());
        if (revokedCount > 0) {
            log.info("Revoked {} existing refresh tokens for user {}", revokedCount, user.getId());
        }

        return issueTokens(user);
    }

    @Transactional
    public AuthResult refresh(String refreshTokenValue) {
        log.info("Token refresh requested");

        RefreshToken refreshToken = refreshTokenRepository
                .findByToken(refreshTokenValue)
                .orElseThrow(() -> new InvalidTokenException("Refresh token is invalid"));

        if (!refreshToken.isValid()) {
            throw new InvalidTokenException("Refresh token is expired or revoked");
        }

        User user = refreshToken.getUser();

        refreshToken.setRevoked(true);
        refreshTokenRepository.save(refreshToken);

        return issueTokens(user);
    }

    private AuthResult issueTokens(User user) {
        String accessToken = jwtService.generateAccessToken(user.getId(), user.getEmail());
        String refreshTokenValue = UUID.randomUUID().toString();

        RefreshToken refreshToken =
                new RefreshToken(refreshTokenValue, user, LocalDateTime.now().plus(refreshTokenExpiry));
        refreshTokenRepository.save(refreshToken);

        return new AuthResult(accessToken, refreshTokenValue, jwtService.getAccessTokenExpirySeconds());
    }
}
