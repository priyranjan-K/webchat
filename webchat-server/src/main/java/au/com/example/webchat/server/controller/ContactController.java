package au.com.example.webchat.server.controller;

import au.com.example.webchat.server.dto.UserDto;
import au.com.example.webchat.server.service.ContactService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;

/**
 * REST controller for contact management in Spring WebFlux.
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
    public Mono<ResponseEntity<String>> addContact(@RequestParam String phone) {
        return currentUserPhone().flatMap(currentUser -> 
            Mono.fromRunnable(() -> contactService.addContact(currentUser, phone))
                .subscribeOn(Schedulers.boundedElastic())
                .then(Mono.just(ResponseEntity.ok("Contact added successfully!")))
                .onErrorResume(IllegalArgumentException.class, e -> 
                    Mono.just(ResponseEntity.badRequest().body(e.getMessage()))
                )
        );
    }

    /**
     * Returns the authenticated user's merged contact list.
     *
     * @return list of {@link UserDto} profiles
     */
    @GetMapping
    public Mono<List<UserDto>> getContacts() {
        return currentUserPhone().flatMap(currentUser ->
            Mono.fromCallable(() -> contactService.getContacts(currentUser))
                .subscribeOn(Schedulers.boundedElastic())
        );
    }

    // ── Internal helper ─────────────────────────────────────────────────────

    private Mono<String> currentUserPhone() {
        return ReactiveSecurityContextHolder.getContext()
                .map(ctx -> (String) ctx.getAuthentication().getPrincipal());
    }
}
