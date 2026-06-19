package au.com.example.webchat.server.config;

/**
 * Application-wide constants.
 * <p>
 * Use this class instead of scattering magic numbers and string literals across
 * the codebase. All values are {@code public static final} so they are inlined
 * at compile-time and carry zero runtime overhead.
 */
public final class AppConstants {

    private AppConstants() {
        // utility class — do not instantiate
    }

    // ── JWT / Token ──────────────────────────────────────────────────────────

    /** Default JWT lifetime: 24 hours in milliseconds. */
    public static final long TOKEN_EXPIRY_MS = 86_400_000L;

    /** HTTP Authorization header prefix. */
    public static final String BEARER_PREFIX = "Bearer ";

    // ── Scheduled Tasks ──────────────────────────────────────────────────────

    /**
     * How often the token-cleanup job runs: every 1 hour in milliseconds.
     * Also used as the initial delay so the job does not run at startup.
     */
    public static final long TOKEN_CLEANUP_RATE_MS = 3_600_000L;

    // ── Password Reset ───────────────────────────────────────────────────────

    /**
     * Mock OTP used during development/demo for the forgot-password flow.
     * <p>
     * <strong>Replace with a real OTP service before going to production.</strong>
     */
    public static final String MOCK_OTP = "123456";

    // ── WebSocket ────────────────────────────────────────────────────────────

    /** WebSocket endpoint path. */
    public static final String WS_ENDPOINT = "/ws/chat";

    // ── API Paths ────────────────────────────────────────────────────────────

    public static final String API_AUTH_BASE      = "/api/auth";
    public static final String API_CONTACTS_BASE  = "/api/contacts";
    public static final String API_MESSAGES_BASE  = "/api/messages";
    public static final String API_USERS_BASE     = "/api/users";
    public static final String HEALTH_ENDPOINT    = "/health";
}
