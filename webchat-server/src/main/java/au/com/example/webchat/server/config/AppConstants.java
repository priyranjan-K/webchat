package au.com.example.webchat.server.config;

/**
 * Application-wide constants — keeps magic strings and numbers in one place.
 */
public final class AppConstants {

    private AppConstants() {}

    // JWT
    /** Default JWT lifetime: 24 hours in milliseconds. */
    public static final long   TOKEN_EXPIRY_MS = 86_400_000L;
    public static final String BEARER_PREFIX   = "Bearer ";

    // Scheduled tasks
    /** Interval for the expired-token cleanup job (1 hour). */
    public static final long TOKEN_CLEANUP_RATE_MS = 3_600_000L;

    // Forgot password — replace with a real OTP/SMS service before going to production
    public static final String MOCK_OTP = "123456";

    // WebSocket
    public static final String WS_ENDPOINT = "/ws/chat";

    // API base paths
    public static final String API_AUTH_BASE     = "/api/auth";
    public static final String API_CONTACTS_BASE = "/api/contacts";
    public static final String API_MESSAGES_BASE = "/api/messages";
    public static final String API_USERS_BASE    = "/api/users";
    public static final String HEALTH_ENDPOINT   = "/health";
}
