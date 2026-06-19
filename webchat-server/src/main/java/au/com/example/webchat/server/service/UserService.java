package au.com.example.webchat.server.service;

import au.com.example.webchat.server.dto.UserDto;
import au.com.example.webchat.server.model.User;
import au.com.example.webchat.server.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Service layer for user profile operations.
 * <p>
 * Provides read-only access to user data (profile look-up, list all users).
 * Write operations (password update, registration) belong to {@link AuthService}.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;

    /**
     * Returns all registered users as lightweight {@link UserDto} objects.
     * Sensitive fields (password, isActive) are never exposed.
     *
     * @return immutable list of user profiles
     */
    public List<UserDto> getAllUsers() {
        return userRepository.findAll()
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    /**
     * Looks up a user by phone number.
     *
     * @param phoneNumber the unique phone identifier
     * @return an {@link Optional} containing the {@link UserDto}, or empty if not found
     */
    public Optional<UserDto> findByPhoneNumber(String phoneNumber) {
        return userRepository.findByPhoneNumber(phoneNumber)
                .map(this::toDto);
    }

    /**
     * Checks whether a phone number is already registered.
     *
     * @param phoneNumber the phone number to check
     * @return {@code true} if the phone number exists in the database
     */
    public boolean existsByPhoneNumber(String phoneNumber) {
        return userRepository.existsByPhoneNumber(phoneNumber);
    }

    // ── Internal helpers ─────────────────────────────────────────────────────

    /**
     * Maps a {@link User} entity to a safe {@link UserDto}.
     *
     * @param user the JPA entity
     * @return DTO with sensitive fields omitted
     */
    public UserDto toDto(User user) {
        return UserDto.builder()
                .phoneNumber(user.getPhoneNumber())
                .displayName(user.getDisplayName())
                .status(user.getStatus())
                .profilePictureUrl(user.getProfilePictureUrl())
                .lastSeen(user.getLastSeen())
                .build();
    }
}
