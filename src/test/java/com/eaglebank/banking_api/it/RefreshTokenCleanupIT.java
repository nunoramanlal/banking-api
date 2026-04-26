package com.eaglebank.banking_api.it;

import static org.assertj.core.api.Assertions.assertThat;

import com.eaglebank.banking_api.entity.RefreshToken;
import com.eaglebank.banking_api.entity.User;
import com.eaglebank.banking_api.scheduler.RefreshTokenCleanup;
import com.eaglebank.banking_api.security.TokenHasher;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class RefreshTokenCleanupIT extends IntegrationUtils {

    @Autowired
    private RefreshTokenCleanup cleanupTask;

    @Autowired
    private TokenHasher tokenHasher;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User(
                TEST_NAME, TEST_EMAIL, TEST_PHONE, TEST_LINE1, null, null, TEST_TOWN, TEST_COUNTY, TEST_POSTCODE);
        userRepository.save(testUser);
    }

    @Test
    void shouldDeleteExpiredTokens() {
        refreshTokenRepository.save(new RefreshToken(
                tokenHasher.hash("expired-token"), testUser, LocalDateTime.now().minusHours(1)));

        cleanupTask.cleanupExpiredTokens();

        assertThat(refreshTokenRepository.findAll()).isEmpty();
    }

    @Test
    void shouldDeleteRevokedTokens() {
        RefreshToken revoked = new RefreshToken(
                tokenHasher.hash("revoked-token"), testUser, LocalDateTime.now().plusDays(7));
        revoked.setRevoked(true);
        refreshTokenRepository.save(revoked);

        cleanupTask.cleanupExpiredTokens();

        assertThat(refreshTokenRepository.findAll()).isEmpty();
    }

    @Test
    void shouldKeepValidTokens() {
        String validHash = tokenHasher.hash("valid-token");
        refreshTokenRepository.save(
                new RefreshToken(validHash, testUser, LocalDateTime.now().plusDays(7)));

        cleanupTask.cleanupExpiredTokens();

        assertThat(refreshTokenRepository.findAll()).hasSize(1);
        assertThat(refreshTokenRepository.findByTokenHash(validHash)).isPresent();
    }

    @Test
    void shouldOnlyDeleteExpiredOrRevokedTokensMixed() {
        String validHash = tokenHasher.hash("valid-token");

        RefreshToken revoked = new RefreshToken(
                tokenHasher.hash("revoked-token"), testUser, LocalDateTime.now().plusDays(7));
        revoked.setRevoked(true);

        refreshTokenRepository.save(new RefreshToken(
                tokenHasher.hash("expired-token"), testUser, LocalDateTime.now().minusHours(1)));
        refreshTokenRepository.save(revoked);
        refreshTokenRepository.save(
                new RefreshToken(validHash, testUser, LocalDateTime.now().plusDays(7)));

        cleanupTask.cleanupExpiredTokens();

        assertThat(refreshTokenRepository.findAll()).hasSize(1);
        assertThat(refreshTokenRepository.findByTokenHash(validHash)).isPresent();
    }

    @Test
    void shouldDoNothingWhenNoTokensExist() {
        cleanupTask.cleanupExpiredTokens();

        assertThat(refreshTokenRepository.findAll()).isEmpty();
    }
}
