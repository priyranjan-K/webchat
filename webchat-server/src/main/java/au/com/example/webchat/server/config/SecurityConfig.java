package au.com.example.webchat.server.config;

import au.com.example.webchat.server.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;
import org.springframework.web.cors.CorsConfiguration;

import java.util.Arrays;
import java.util.List;

/**
 * Spring Security configuration for Spring WebFlux.
 * <p>
 * Stateless JWT-based authentication with explicit public/protected endpoint rules.
 * CORS allowed origins are driven by the {@code cors.allowed-origins} property so
 * that dev and production environments can differ without code changes.
 */
@Configuration
@EnableWebFluxSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    /**
     * Allowed origins loaded from {@code cors.allowed-origins} in application.yml.
     * Defaults to localhost ports used during development.
     */
    @Value("${cors.allowed-origins:http://localhost:3000,http://localhost:9876}")
    private List<String> allowedOrigins;

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(allowedOrigins);
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type", "X-Requested-With"));
        configuration.setExposedHeaders(Arrays.asList("Authorization", "Content-Type"));
        configuration.setAllowCredentials(false);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(csrf -> csrf.disable())
            .authorizeExchange(exchanges -> exchanges
                // Public endpoints — no auth required
                .pathMatchers(
                    AppConstants.API_AUTH_BASE + "/signup",
                    AppConstants.API_AUTH_BASE + "/login",
                    AppConstants.API_AUTH_BASE + "/forgot-password/**",
                    AppConstants.API_AUTH_BASE + "/hello",
                    AppConstants.HEALTH_ENDPOINT,
                    "/"
                ).permitAll()
                // All other endpoints require a valid JWT
                .anyExchange().authenticated()
            )
            .addFilterBefore(jwtAuthenticationFilter, SecurityWebFiltersOrder.AUTHENTICATION);

        return http.build();
    }
}
