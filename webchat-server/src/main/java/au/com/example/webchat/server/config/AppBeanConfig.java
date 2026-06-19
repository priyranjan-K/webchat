package au.com.example.webchat.server.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * General application bean definitions.
 * <p>
 * Kept intentionally small — each bean here should not have a more specific
 * configuration class it belongs to (e.g. security beans live in
 * {@link SecurityConfig}, WebSocket beans in {@link WebSocketConfig}).
 */
@Configuration
public class AppBeanConfig {

    /**
     * BCrypt password encoder with default strength (10 rounds).
     * Injected into {@link au.com.example.webchat.server.service.AuthService}.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
