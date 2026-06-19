package au.com.example.webchat.server.controller;

import au.com.example.webchat.server.dto.UserDto;
import au.com.example.webchat.server.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST controller for user profile operations.
 * <p>
 * Currently exposes a single endpoint to retrieve all registered users
 * (used by the client to resolve display names for contacts).
 * All logic is delegated to {@link UserService}.
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
    public List<UserDto> getAllUsers() {
        return userService.getAllUsers();
    }
}
