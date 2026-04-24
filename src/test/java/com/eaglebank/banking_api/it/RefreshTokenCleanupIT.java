package com.eaglebank.banking_api.it;

import static org.assertj.core.api.Assertions.assertThat;

import com.eaglebank.banking_api.entity.RefreshToken;
import com.eaglebank.banking_api.entity.User;
import com.eaglebank.banking_api.repository.RefreshTokenRepository;
import com.eaglebank.banking_api.repository.UserRepository;
import com.eaglebank.banking_api.scheduler.RefreshTokenCleanup;
import java.time.LocalDateTime;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class RefreshTokenCleanupIT {

    @Autowired
    private RefreshTokenCleanup cleanupTask;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private UserRepository userRepository;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User(
                "test-name",
                "test@example.com",
                "+447911123456",
                "test-line1",
                null,
                null,
                "test-town",
                "test-county",
                "TEST 123");
        userRepository.save(testUser);
    }

    @AfterEach
    void cleanUp() {
        refreshTokenRepository.deleteAll();
        userRepository.deleteAll();
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
