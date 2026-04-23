package com.eaglebank.banking_api.it;

import static org.assertj.core.api.Assertions.assertThat;

import com.eaglebank.banking_api.entity.RefreshToken;
import com.eaglebank.banking_api.repository.RefreshTokenRepository;
import com.eaglebank.banking_api.scheduler.RefreshTokenCleanup;
import java.time.LocalDateTime;
import org.junit.jupiter.api.AfterEach;
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

    @AfterEach
    void cleanUp() {
        refreshTokenRepository.deleteAll();
    }

    @Test
    void shouldDeleteExpiredTokens() {
        RefreshToken expired =
                new RefreshToken("expired-token", "usr-1", LocalDateTime.now().minusHours(1));
        refreshTokenRepository.save(expired);

        cleanupTask.cleanupExpiredTokens();

        assertThat(refreshTokenRepository.findAll()).isEmpty();
    }

    @Test
    void shouldDeleteRevokedTokens() {
        RefreshToken revoked =
                new RefreshToken("revoked-token", "usr-1", LocalDateTime.now().plusDays(7));
        revoked.setRevoked(true);
        refreshTokenRepository.save(revoked);

        cleanupTask.cleanupExpiredTokens();

        assertThat(refreshTokenRepository.findAll()).isEmpty();
    }

    @Test
    void shouldKeepValidTokens() {
        RefreshToken valid =
                new RefreshToken("valid-token", "usr-1", LocalDateTime.now().plusDays(7));
        refreshTokenRepository.save(valid);

        cleanupTask.cleanupExpiredTokens();

        assertThat(refreshTokenRepository.findAll()).hasSize(1);
        assertThat(refreshTokenRepository.findByToken("valid-token")).isPresent();
    }

    @Test
    void shouldOnlyDeleteExpiredOrRevokedTokensMixed() {
        RefreshToken expired =
                new RefreshToken("expired-token", "usr-1", LocalDateTime.now().minusHours(1));
        RefreshToken revoked =
                new RefreshToken("revoked-token", "usr-1", LocalDateTime.now().plusDays(7));
        revoked.setRevoked(true);
        RefreshToken valid =
                new RefreshToken("valid-token", "usr-1", LocalDateTime.now().plusDays(7));

        refreshTokenRepository.save(expired);
        refreshTokenRepository.save(revoked);
        refreshTokenRepository.save(valid);

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
