package com.eaglebank.banking_api.it;

import static org.assertj.core.api.Assertions.assertThat;

import com.eaglebank.banking_api.entity.RefreshToken;
import com.eaglebank.banking_api.entity.User;
import com.eaglebank.banking_api.scheduler.RefreshTokenCleanup;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class RefreshTokenCleanupIT extends IntegrationUtils {

    @Autowired
    private RefreshTokenCleanup cleanupTask;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User(
                TEST_NAME, TEST_EMAIL, TEST_PHONE, TEST_LINE1, null, null, TEST_TOWN, TEST_COUNTY, TEST_POSTCODE);
        userRepository.save(testUser);
    }

    @Test
    void shouldDeleteExpiredTokens() {
        refreshTokenRepository.save(
                new RefreshToken("expired-token", testUser, LocalDateTime.now().minusHours(1)));

        cleanupTask.cleanupExpiredTokens();

        assertThat(refreshTokenRepository.findAll()).isEmpty();
    }

    @Test
    void shouldDeleteRevokedTokens() {
        RefreshToken revoked =
                new RefreshToken("revoked-token", testUser, LocalDateTime.now().plusDays(7));
        revoked.setRevoked(true);
        refreshTokenRepository.save(revoked);

        cleanupTask.cleanupExpiredTokens();

        assertThat(refreshTokenRepository.findAll()).isEmpty();
    }

    @Test
    void shouldKeepValidTokens() {
        refreshTokenRepository.save(
                new RefreshToken("valid-token", testUser, LocalDateTime.now().plusDays(7)));

        cleanupTask.cleanupExpiredTokens();

        assertThat(refreshTokenRepository.findAll()).hasSize(1);
        assertThat(refreshTokenRepository.findByToken("valid-token")).isPresent();
    }

    @Test
    void shouldOnlyDeleteExpiredOrRevokedTokensMixed() {
        RefreshToken revoked =
                new RefreshToken("revoked-token", testUser, LocalDateTime.now().plusDays(7));
        revoked.setRevoked(true);

        refreshTokenRepository.save(
                new RefreshToken("expired-token", testUser, LocalDateTime.now().minusHours(1)));
        refreshTokenRepository.save(revoked);
        refreshTokenRepository.save(
                new RefreshToken("valid-token", testUser, LocalDateTime.now().plusDays(7)));

        cleanupTask.cleanupExpiredTokens();

        assertThat(refreshTokenRepository.findAll()).hasSize(1);
        assertThat(refreshTokenRepository.findByToken("valid-token")).isPresent();
    }

    @Test
    void shouldDoNothingWhenNoTokensExist() {
        cleanupTask.cleanupExpiredTokens();

        assertThat(refreshTokenRepository.findAll()).isEmpty();
    }
}
