package au.com.example.webchat.server.service;

import au.com.example.webchat.server.config.AppConstants;
import au.com.example.webchat.server.repository.JwtTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * Scheduled service that purges expired JWT tokens from the database.
 * <p>
 * Runs every hour ({@value AppConstants#TOKEN_CLEANUP_RATE_MS} ms) with an equal initial
 * delay so the cleanup does not execute at application startup.
 * The rate can be overridden via the {@code token.cleanup.rate-ms} property.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TokenCleanupService {

    private final JwtTokenRepository jwtTokenRepository;

    @Scheduled(
        fixedRateString    = "${token.cleanup.rate-ms:3600000}",
        initialDelayString = "${token.cleanup.rate-ms:3600000}"
    )
    public void cleanExpiredTokens() {
        long now = System.currentTimeMillis();
        try {
            jwtTokenRepository.deleteByExpiryLessThan(now);
            log.debug("Expired JWT tokens cleaned up at epoch {}", now);
        } catch (Exception e) {
            log.error("Failed to clean up expired JWT tokens", e);
        }
    }
}
