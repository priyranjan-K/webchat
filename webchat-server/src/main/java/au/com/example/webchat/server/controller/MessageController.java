package au.com.example.webchat.server.controller;

import au.com.example.webchat.server.model.MessageEntity;
import au.com.example.webchat.server.service.MessageService;
import au.com.example.webchat.server.service.GroupService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/**
 * REST controller for message history retrieval.
 * <p>
 * Provides endpoints to fetch historical messages for direct conversations
 * and group chats. Real-time message delivery happens over WebSocket.
 * All logic is delegated to {@link MessageService}.
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
    public List<MessageEntity> getDmHistory(@PathVariable String counterpart) {
        String currentUser = currentUserPhone();
        return messageService.getDmHistory(currentUser, counterpart);
    }

    /**
     * Returns all messages for a group conversation.
     *
     * @param groupId the group identifier
     * @return ordered list of message entities (ascending by timestamp)
     */
    @GetMapping("/group/{groupId}")
    public List<MessageEntity> getGroupHistory(@PathVariable String groupId) {
        String currentUser = currentUserPhone();
        boolean isMember = groupService.findGroup(groupId)
                .map(group -> group.hasMember(currentUser))
                .orElse(false);
        if (!isMember) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not a member of this group");
        }
        return messageService.getGroupHistory(groupId);
    }

    // ── Internal helper ─────────────────────────────────────────────────────

    private String currentUserPhone() {
        return (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }
}
