package au.com.example.webchat.server.controller;

import au.com.example.webchat.server.dto.UserDto;
import au.com.example.webchat.server.service.ContactService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for contact management.
 * <p>
 * Exposes endpoints for adding contacts and retrieving a user's contact list.
 * All business logic is delegated to {@link ContactService}.
 */
@RestController
@RequestMapping("/api/contacts")
@RequiredArgsConstructor
@Slf4j
public class ContactController {

    private final ContactService contactService;

    /**
     * Adds a contact by phone number for the currently authenticated user.
     *
     * @param phone the target contact's phone number
     * @return {@code 200 OK} on success, {@code 400 Bad Request} if validation fails
     */
    @PostMapping("/add")
    public ResponseEntity<String> addContact(@RequestParam String phone) {
        String currentUser = currentUserPhone();
        try {
            contactService.addContact(currentUser, phone);
            return ResponseEntity.ok("Contact added successfully!");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * Returns the authenticated user's merged contact list:
     * explicitly added contacts plus inferred DM partners.
     *
     * @return list of {@link UserDto} profiles
     */
    @GetMapping
    public List<UserDto> getContacts() {
        return contactService.getContacts(currentUserPhone());
    }

    // ── Internal helper ─────────────────────────────────────────────────────

    private String currentUserPhone() {
        return (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }
}
