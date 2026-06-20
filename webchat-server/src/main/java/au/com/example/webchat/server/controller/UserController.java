package au.com.example.webchat.server.controller;

import au.com.example.webchat.server.dto.UserDto;
import au.com.example.webchat.server.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;

/**
 * REST controller for user profile operations in Spring WebFlux.
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /**
     * Returns all registered users as lightweight profile DTOs.
     * Sensitive fields (password, isActive) are never included.
     *
     * @return list of {@link UserDto} profiles
     */
    @GetMapping
    public Mono<List<UserDto>> getAllUsers() {
        return Mono.fromCallable(() -> userService.getAllUsers())
                .subscribeOn(Schedulers.boundedElastic());
    }
}
