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
import au.com.example.webchat.server.service.RsaKeyService;
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
    private final RsaKeyService      rsaKeyService;

    /**
     * Registers a new user and issues a JWT on success.
     *
     * @param request sign-up form data
     * @return {@link AuthResponse} with the token, or an error message
     */
    public AuthResponse signup(SignUpRequest request) {
        String phone = rsaKeyService.decrypt(request.getPhoneNumber());
        String password = rsaKeyService.decrypt(request.getPassword());

        if (userRepository.existsByPhoneNumber(phone)) {
            return AuthResponse.builder()
                    .message("Phone number already registered!")
                    .build();
        }

        User user = User.builder()
                .phoneNumber(phone)
                .displayName(request.getDisplayName())
                .password(passwordEncoder.encode(password))
                .build();
        userRepository.save(user);

        String token = issueToken(user.getPhoneNumber());
        log.info("User registered: {}", phone);

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
        String phone = rsaKeyService.decrypt(request.getPhoneNumber());
        String password = rsaKeyService.decrypt(request.getPassword());

        User user = userRepository.findByPhoneNumber(phone).orElse(null);

        if (user == null || !passwordEncoder.matches(password, user.getPassword())) {
            return AuthResponse.builder()
                    .message("Invalid phone number or password!")
                    .build();
        }

        user.setLastSeen(System.currentTimeMillis());
        userRepository.save(user);

        String token = issueToken(user.getPhoneNumber());
        log.info("User logged in: {}", phone);

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
        String phone = rsaKeyService.decrypt(phoneNumber);
        if (!userRepository.existsByPhoneNumber(phone)) {
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
        String phone = rsaKeyService.decrypt(request.getPhoneNumber());
        String newPassword = rsaKeyService.decrypt(request.getNewPassword());

        User user = userRepository.findByPhoneNumber(phone).orElse(null);
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

        if (newPassword == null || newPassword.isEmpty()) {
            return AuthResponse.builder()
                    .message("New password is required!")
                    .build();
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        log.info("Password reset for user: {}", phone);

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
