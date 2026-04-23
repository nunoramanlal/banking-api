package com.eaglebank.banking_api.scheduler;

import com.eaglebank.banking_api.repository.RefreshTokenRepository;
import java.time.LocalDateTime;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
public class RefreshTokenCleanup {

    private final RefreshTokenRepository refreshTokenRepository;

    public RefreshTokenCleanup(RefreshTokenRepository refreshTokenRepository) {
        this.refreshTokenRepository = refreshTokenRepository;
    }

    @Scheduled(cron = "${security.jwt.cleanup-cron:0 0 * * * *}")
    @Transactional
    public void cleanupExpiredTokens() {
        int deleted = refreshTokenRepository.deleteExpiredOrRevoked(LocalDateTime.now());
        if (deleted > 0) {
            log.info("Cleaned up {} expired or revoked refresh tokens", deleted);
        }
    }
}
