package au.com.example.webchat.server.service;

import au.com.example.webchat.server.config.AppConstants;
import au.com.example.webchat.server.dto.AuthResponse;
import au.com.example.webchat.server.dto.LoginRequest;
import au.com.example.webchat.server.dto.PasswordResetRequest;
import au.com.example.webchat.server.dto.SignUpRequest;
import au.com.example.webchat.server.model.JwtToken;
import au.com.example.webchat.server.model.User;
import au.com.example.webchat.server.repository.JwtTokenRepository;
import au.com.example.webchat.server.repository.UserRepository;
import au.com.example.webchat.server.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * Handles user registration, login, logout, and the password-reset flow.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository     userRepository;
    private final PasswordEncoder    passwordEncoder;
    private final JwtTokenProvider   jwtTokenProvider;
    private final JwtTokenRepository jwtTokenRepository;

    /**
     * Registers a new user and issues a JWT on success.
     *
     * @param request sign-up form data
     * @return {@link AuthResponse} with the token, or an error message
     */
    public AuthResponse signup(SignUpRequest request) {
        if (userRepository.existsByPhoneNumber(request.getPhoneNumber())) {
            return AuthResponse.builder()
                    .message("Phone number already registered!")
                    .build();
        }

        User user = User.builder()
                .phoneNumber(request.getPhoneNumber())
                .displayName(request.getDisplayName())
                .password(passwordEncoder.encode(request.getPassword()))
                .build();
        userRepository.save(user);

        String token = issueToken(user.getPhoneNumber());
        log.info("User registered: {}", request.getPhoneNumber());

        return AuthResponse.builder()
                .token(token)
                .phoneNumber(user.getPhoneNumber())
                .displayName(user.getDisplayName())
                .message("Signup successful!")
                .build();
    }

    /**
     * Authenticates an existing user and issues a fresh JWT.
     *
     * @param request login credentials
     * @return {@link AuthResponse} with the token, or an error message
     */
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByPhoneNumber(request.getPhoneNumber()).orElse(null);

        if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            return AuthResponse.builder()
                    .message("Invalid phone number or password!")
                    .build();
        }

        user.setLastSeen(System.currentTimeMillis());
        userRepository.save(user);

        String token = issueToken(user.getPhoneNumber());
        log.info("User logged in: {}", request.getPhoneNumber());

        return AuthResponse.builder()
                .token(token)
                .phoneNumber(user.getPhoneNumber())
                .displayName(user.getDisplayName())
                .message("Login successful!")
                .build();
    }

    /**
     * Revokes all active JWT tokens for the given user (logout).
     *
     * @param phoneNumber the authenticated user's phone number
     */
    public void logout(String phoneNumber) {
        jwtTokenRepository.deleteByPhoneNumber(phoneNumber);
        log.info("Tokens revoked for user: {}", phoneNumber);
    }

    /**
     * Initiates a password-reset request.
     * <p>Currently returns a mock OTP ({@value AppConstants#MOCK_OTP}).
     * Replace with a real OTP/SMS provider before deploying to production.</p>
     *
     * @param phoneNumber the user's registered phone number
     * @return {@link AuthResponse} with instructions or an error message
     */
    public AuthResponse requestPasswordReset(String phoneNumber) {
        if (!userRepository.existsByPhoneNumber(phoneNumber)) {
            return AuthResponse.builder()
                    .message("Phone number is not registered!")
                    .build();
        }
        return AuthResponse.builder()
                .message("Verification code sent! Use mock code " + AppConstants.MOCK_OTP + ".")
                .build();
    }

    /**
     * Resets the user's password after OTP verification.
     *
     * @param request phone number, OTP, and new password
     * @return {@link AuthResponse} indicating success or failure
     */
    public AuthResponse resetPassword(PasswordResetRequest request) {
        User user = userRepository.findByPhoneNumber(request.getPhoneNumber()).orElse(null);
        if (user == null) {
            return AuthResponse.builder()
                    .message("Phone number is not registered!")
                    .build();
        }

        if (!AppConstants.MOCK_OTP.equals(request.getOtp())) {
            return AuthResponse.builder()
                    .message("Invalid verification code!")
                    .build();
        }

        if (request.getNewPassword() == null || request.getNewPassword().isEmpty()) {
            return AuthResponse.builder()
                    .message("New password is required!")
                    .build();
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        log.info("Password reset for user: {}", request.getPhoneNumber());

        return AuthResponse.builder()
                .message("Password reset successful!")
                .build();
    }

    private String issueToken(String phoneNumber) {
        String token = jwtTokenProvider.generateToken(phoneNumber);
        jwtTokenRepository.save(JwtToken.builder()
                .token(token)
                .phoneNumber(phoneNumber)
                .expiry(System.currentTimeMillis() + AppConstants.TOKEN_EXPIRY_MS)
                .build());
        return token;
    }
}
