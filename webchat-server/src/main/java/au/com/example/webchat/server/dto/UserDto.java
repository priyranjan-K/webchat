package au.com.example.webchat.server.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data Transfer Object representing a user profile summary.
 * <p>
 * Returned by {@code /api/users} and embedded in contact list responses.
 * Does NOT expose sensitive fields (password, isActive, etc.).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserDto {

    private String phoneNumber;

    private String displayName;

    private String status;

    private String profilePictureUrl;

    /** Epoch-millis timestamp of the user's last activity, may be {@code null}. */
    private Long lastSeen;
}
