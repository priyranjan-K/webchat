package au.com.example.webchat.server.config;

import au.com.example.webchat.server.websocket.ChatWebSocketHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * WebSocket endpoint registration.
 * <p>
 * Registers {@link ChatWebSocketHandler} at the path defined in
 * {@link AppConstants#WS_ENDPOINT}. Allowed origins are property-driven
 * to match the CORS configuration in {@link SecurityConfig}.
 */
@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketConfigurer {

    private final ChatWebSocketHandler chatWebSocketHandler;

    @Value("${cors.allowed-origins:http://localhost:3000,http://localhost:9876}")
    private String[] allowedOrigins;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(chatWebSocketHandler, AppConstants.WS_ENDPOINT)
                .setAllowedOrigins(allowedOrigins);
    }
}
