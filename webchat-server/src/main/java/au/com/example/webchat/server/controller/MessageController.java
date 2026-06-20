package au.com.example.webchat.server.controller;

import au.com.example.webchat.server.model.MessageEntity;
import au.com.example.webchat.server.service.MessageService;
import au.com.example.webchat.server.service.GroupService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;

/**
 * REST controller for message history retrieval in Spring WebFlux.
 */
@RestController
@RequestMapping("/api/messages")
@RequiredArgsConstructor
public class MessageController {

    private final MessageService messageService;
    private final GroupService groupService;

    /**
     * Returns the direct-message history between the authenticated user and another user.
     *
     * @param counterpart phone number of the other participant
     * @return ordered list of message entities (ascending by timestamp)
     */
    @GetMapping("/dm/{counterpart}")
    public Mono<List<MessageEntity>> getDmHistory(@PathVariable String counterpart) {
        return currentUserPhone().flatMap(currentUser ->
            Mono.fromCallable(() -> messageService.getDmHistory(currentUser, counterpart))
                .subscribeOn(Schedulers.boundedElastic())
        );
    }

    /**
     * Returns all messages for a group conversation.
     *
     * @param groupId the group identifier
     * @return ordered list of message entities (ascending by timestamp)
     */
    @GetMapping("/group/{groupId}")
    public Mono<List<MessageEntity>> getGroupHistory(@PathVariable String groupId) {
        return currentUserPhone().flatMap(currentUser ->
            Mono.fromCallable(() -> groupService.findGroup(groupId))
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(optGroup -> {
                    boolean isMember = optGroup
                            .map(group -> group.hasMember(currentUser) || (group.getLeftMembers() != null && group.getLeftMembers().contains(currentUser)))
                            .orElse(false);
                    if (!isMember) {
                        return Mono.error(new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not a member of this group"));
                    }
                    return Mono.fromCallable(() -> messageService.getGroupHistory(groupId))
                            .subscribeOn(Schedulers.boundedElastic());
                })
        );
    }

    // ── Internal helper ─────────────────────────────────────────────────────

    private Mono<String> currentUserPhone() {
        return ReactiveSecurityContextHolder.getContext()
                .map(ctx -> (String) ctx.getAuthentication().getPrincipal());
    }
}
