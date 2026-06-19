package au.com.example.webchat.server.controller;

import au.com.example.webchat.server.dto.AuthResponse;
import au.com.example.webchat.server.dto.LoginRequest;
import au.com.example.webchat.server.dto.PasswordResetRequest;
import au.com.example.webchat.server.dto.SignUpRequest;
import au.com.example.webchat.server.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for authentication operations.
 * <p>
 * Public endpoints: {@code /signup}, {@code /login}, {@code /forgot-password/**}<br>
 * Authenticated endpoint: {@code /logout}
 * <p>
 * All business logic is delegated to {@link AuthService}.
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthService authService;

    /** Quick health check — listed as public in {@code SecurityConfig}. */
    @GetMapping("/hello")
    public String hello() {
        return "Hello, welcome to WebChat!";
    }

    @PostMapping("/signup")
    public ResponseEntity<AuthResponse> signup(@Valid @RequestBody SignUpRequest request) {
        try {
            AuthResponse response = authService.signup(request);

            if (response.getToken() == null) {
                return ResponseEntity.badRequest().body(response);
            }

            log.info("User signed up: {}", request.getPhoneNumber());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Signup error", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(AuthResponse.builder().message("Signup failed!").build());
        }
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        try {
            AuthResponse response = authService.login(request);

            if (response.getToken() == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }

            log.info("User logged in: {}", request.getPhoneNumber());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Login error", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(AuthResponse.builder().message("Login failed!").build());
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<String> logout() {
        String currentUser = (String) SecurityContextHolder.getContext()
                .getAuthentication()
                .getPrincipal();
        if (currentUser != null) {
            authService.logout(currentUser);
            log.info("User logged out (tokens cleared): {}", currentUser);
        }
        return ResponseEntity.ok("Logged out successfully!");
    }

    @PostMapping("/forgot-password/request")
    public ResponseEntity<AuthResponse> requestPasswordReset(@RequestParam String phoneNumber) {
        try {
            AuthResponse response = authService.requestPasswordReset(phoneNumber);
            if (response.getMessage().contains("not registered")) {
                return ResponseEntity.badRequest().body(response);
            }
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Request password reset error", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(AuthResponse.builder().message("Request reset failed!").build());
        }
    }

    @PostMapping("/forgot-password/reset")
    public ResponseEntity<AuthResponse> resetPassword(@Valid @RequestBody PasswordResetRequest request) {
        try {
            AuthResponse response = authService.resetPassword(request);
            if (response.getMessage().contains("successful")) {
                return ResponseEntity.ok(response);
            }
            return ResponseEntity.badRequest().body(response);
        } catch (Exception e) {
            log.error("Reset password error", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(AuthResponse.builder().message("Password reset failed!").build());
        }
    }
}
