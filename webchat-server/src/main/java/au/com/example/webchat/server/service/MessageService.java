package au.com.example.webchat.server.service;

import au.com.example.webchat.server.model.MessageEntity;
import au.com.example.webchat.server.repository.MessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Service layer for message retrieval operations.
 * <p>
 * Provides history look-up for both direct messages and group conversations.
 * Message <em>persistence</em> (saving new messages) is handled by
 * {@link au.com.example.webchat.server.websocket.ChatWebSocketHandler}
 * since it occurs in the real-time WebSocket flow.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MessageService {

    private final MessageRepository messageRepository;

    /**
     * Returns the full direct-message history between two users, ordered by timestamp ascending.
     *
     * @param userPhone      the authenticated user's phone number
     * @param counterpart    the other participant's phone number
     * @return ordered list of message entities
     */
    public List<MessageEntity> getDmHistory(String userPhone, String counterpart) {
        log.debug("Fetching DM history between {} and {}", userPhone, counterpart);
        return messageRepository.findDmHistory(userPhone, counterpart);
    }

    /**
     * Returns all messages for a group conversation, ordered by timestamp ascending.
     *
     * @param groupId the group identifier
     * @return ordered list of message entities
     */
    public List<MessageEntity> getGroupHistory(String groupId) {
        log.debug("Fetching group message history for groupId={}", groupId);
        return messageRepository.findByGroupIdOrderByTimestampAsc(groupId);
    }
}
