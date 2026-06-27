package au.com.example.webchat.server.controller;

import au.com.example.webchat.server.dto.AuthResponse;
import au.com.example.webchat.server.dto.LoginRequest;
import au.com.example.webchat.server.dto.PasswordResetRequest;
import au.com.example.webchat.server.dto.SignUpRequest;
import au.com.example.webchat.server.dto.PublicKeyResponse;
import au.com.example.webchat.server.service.AuthService;
import au.com.example.webchat.server.service.RsaKeyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/**
 * REST controller for authentication operations in Spring WebFlux.
 * <p>
 * Public endpoints: {@code /signup}, {@code /login}, {@code /forgot-password/**}<br>
 * Authenticated endpoint: {@code /logout}
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthService authService;
    private final RsaKeyService rsaKeyService;

    @GetMapping("/public-key")
    public Mono<ResponseEntity<PublicKeyResponse>> getPublicKey() {
        return Mono.fromCallable(() -> ResponseEntity.ok(
                PublicKeyResponse.builder()
                        .publicKey(rsaKeyService.getPublicKeyPem())
                        .build()
        )).subscribeOn(Schedulers.boundedElastic());
    }

    /** Quick health check — listed as public in {@code SecurityConfig}. */
    @GetMapping("/hello")
    public Mono<String> hello() {
        return Mono.just("Hello, welcome to WebChat!");
    }

    @PostMapping("/signup")
    public Mono<ResponseEntity<AuthResponse>> signup(@Valid @RequestBody SignUpRequest request) {
        return Mono.fromCallable(() -> authService.signup(request))
                .subscribeOn(Schedulers.boundedElastic())
                .map(response -> {
                    if (response.getToken() == null) {
                        return ResponseEntity.badRequest().body(response);
                    }
                    log.info("User signed up: {}", request.getPhoneNumber());
                    return ResponseEntity.ok(response);
                })
                .onErrorResume(e -> {
                    log.error("Signup error", e);
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .body(AuthResponse.builder().message("Signup failed!").build()));
                });
    }

    @PostMapping("/login")
    public Mono<ResponseEntity<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        return Mono.fromCallable(() -> authService.login(request))
                .subscribeOn(Schedulers.boundedElastic())
                .map(response -> {
                    if (response.getToken() == null) {
                        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
                    }
                    log.info("User logged in: {}", request.getPhoneNumber());
                    return ResponseEntity.ok(response);
                })
                .onErrorResume(e -> {
                    log.error("Login error", e);
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .body(AuthResponse.builder().message("Login failed!").build()));
                });
    }

    @PostMapping("/logout")
    public Mono<ResponseEntity<String>> logout() {
        return ReactiveSecurityContextHolder.getContext()
                .map(ctx -> (String) ctx.getAuthentication().getPrincipal())
                .flatMap(currentUser -> Mono.fromRunnable(() -> authService.logout(currentUser))
                        .subscribeOn(Schedulers.boundedElastic())
                        .then(Mono.fromCallable(() -> {
                            log.info("User logged out (tokens cleared): {}", currentUser);
                            return ResponseEntity.ok("Logged out successfully!");
                        }))
                )
                .defaultIfEmpty(ResponseEntity.ok("Logged out successfully!"));
    }

    @PostMapping("/forgot-password/request")
    public Mono<ResponseEntity<AuthResponse>> requestPasswordReset(@RequestParam String phoneNumber) {
        return Mono.fromCallable(() -> authService.requestPasswordReset(phoneNumber))
                .subscribeOn(Schedulers.boundedElastic())
                .map(response -> {
                    if (response.getMessage().contains("not registered")) {
                        return ResponseEntity.badRequest().body(response);
                    }
                    return ResponseEntity.ok(response);
                })
                .onErrorResume(e -> {
                    log.error("Request password reset error", e);
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .body(AuthResponse.builder().message("Request reset failed!").build()));
                });
    }

    @PostMapping("/forgot-password/reset")
    public Mono<ResponseEntity<AuthResponse>> resetPassword(@Valid @RequestBody PasswordResetRequest request) {
        return Mono.fromCallable(() -> authService.resetPassword(request))
                .subscribeOn(Schedulers.boundedElastic())
                .map(response -> {
                    if (response.getMessage().contains("successful")) {
                        return ResponseEntity.ok(response);
                    }
                    return ResponseEntity.badRequest().body(response);
                })
                .onErrorResume(e -> {
                    log.error("Reset password error", e);
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .body(AuthResponse.builder().message("Password reset failed!").build()));
                });
    }
}
