package au.com.example.webchat.server.websocket;

import au.com.example.webchat.server.dto.ChatMessage;
import au.com.example.webchat.server.model.MessageEntity;
import au.com.example.webchat.server.repository.ContactRepository;
import au.com.example.webchat.server.repository.MessageRepository;
import au.com.example.webchat.server.repository.UserRepository;
import au.com.example.webchat.server.service.GroupService;
import au.com.example.webchat.server.service.KafkaMessagingService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Component
@Slf4j
public class ReactiveChatWebSocketHandler implements WebSocketHandler {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final GroupService groupService;
    private final MessageRepository messageRepository;
    private final UserRepository userRepository;
    private final ContactRepository contactRepository;
    private final KafkaMessagingService kafkaMessagingService;

    // Session ID -> Sink for writing reactive messages
    private final Map<String, Sinks.Many<String>> sessionSinks = new ConcurrentHashMap<>();

    // Phone -> Set of active session IDs (supports multiple tabs)
    private final Map<String, Set<String>> phoneToSessions = new ConcurrentHashMap<>();

    // Session ID -> Phone number (for inverse lookup)
    private final Map<String, String> sessionToPhone = new ConcurrentHashMap<>();

    // Group ID -> Set of Session IDs currently in the room
    private final Map<String, Set<String>> groupSessions = new ConcurrentHashMap<>();

    /**
     * Tracks messageIds that have already been delivered to local WebSocket sinks.
     * When Kafka delivers the same message back (for cross-server/audit), we skip
     * re-delivery to prevent duplicate messages appearing in the chat.
     * Entries are automatically evicted after 30 seconds to avoid memory leaks.
     */
    private final Set<String> locallyDeliveredIds = ConcurrentHashMap.newKeySet();
    private final ScheduledExecutorService evictionScheduler =
            Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "local-delivery-eviction");
                t.setDaemon(true);
                return t;
            });

    public ReactiveChatWebSocketHandler(
            GroupService groupService,
            MessageRepository messageRepository,
            UserRepository userRepository,
            ContactRepository contactRepository,
            @Lazy KafkaMessagingService kafkaMessagingService) {
        this.groupService = groupService;
        this.messageRepository = messageRepository;
        this.userRepository = userRepository;
        this.contactRepository = contactRepository;
        this.kafkaMessagingService = kafkaMessagingService;
    }

    @Override
    public Mono<Void> handle(WebSocketSession session) {
        String sessionId = session.getId();
        Sinks.Many<String> sink = Sinks.many().multicast().onBackpressureBuffer();
        sessionSinks.put(sessionId, sink);

        log.info("Reactive WebSocket connection opened: sessionId={}", sessionId);

        Mono<Void> receiveMono = session.receive()
                .map(WebSocketMessage::getPayloadAsText)
                .flatMap(payload -> handleMessage(session, payload))
                .then();

        Mono<Void> sendMono = session.send(sink.asFlux().map(session::textMessage));

        return Mono.zip(receiveMono, sendMono)
                .then()
                .doFinally(signalType -> handleDisconnect(session));
    }

    private Mono<Void> handleMessage(WebSocketSession session, String payload) {
        try {
            ChatMessage msg = objectMapper.readValue(payload, ChatMessage.class);
            log.debug("Message received: type={} sender={}", msg.getType(), msg.getSender());

            switch (msg.getType()) {
                case "REGISTER"            -> handleRegister(session, msg);
                case "JOIN"                -> handleJoin(session, msg);
                case "LEAVE"               -> handleLeave(session, msg);
                case "CREATE_GROUP"        -> handleCreateGroup(session, msg);
                case "TEXT"                -> handleGroupText(session, msg);
                case "DIRECT_MESSAGE"      -> handleDirectMessage(session, msg);
                case "LIST_GROUPS"         -> handleListGroups(session);
                case "LIST_USERS"          -> handleListUsers(session, msg);
                case "LIST_ONLINE_USERS"   -> handleListOnlineUsers(session);
                case "DELETE_GROUP"        -> handleDeleteGroup(session, msg);
                case "ADD_GROUP_MEMBER"    -> handleAddGroupMember(session, msg);
                case "REMOVE_GROUP_MEMBER" -> handleRemoveGroupMember(session, msg);
                case "PROMOTE_MEMBER"      -> handlePromoteMember(session, msg);
                case "DEMOTE_MEMBER"       -> handleDemoteMember(session, msg);
                case "DELETE_DM"           -> handleDeleteDm(session, msg);
                default -> log.warn("Unknown message type received: {}", msg.getType());
            }
        } catch (Exception e) {
            log.error("Error processing WebSocket message", e);
        }
        return Mono.empty();
    }

    private void handleDisconnect(WebSocketSession session) {
        String sessionId = session.getId();
        sessionSinks.remove(sessionId);
        String phone = sessionToPhone.remove(sessionId);

        if (phone != null) {
            Set<String> sessions = phoneToSessions.get(phone);
            if (sessions != null) {
                sessions.remove(sessionId);
                if (sessions.isEmpty()) {
                    phoneToSessions.remove(phone);
                }
            }
            log.info("User {} disconnected (sessionId={})", phone, sessionId);
        }

        // Clean up group sessions
        groupSessions.forEach((groupId, sessions) -> {
            if (sessions.remove(sessionId)) {
                ChatMessage leaveMsg = ChatMessage.builder()
                        .sender("SERVER")
                        .type("LEAVE")
                        .groupId(groupId)
                        .content((phone != null ? phone : "Someone") + " disconnected!")
                        .timestamp(System.currentTimeMillis())
                        .status("SUCCESS")
                        .build();
                kafkaMessagingService.publishMessage(leaveMsg);
            }
        });
    }

    // ── Local Request-Response Helpers ────────────────────────────────────────

    private void sendToSession(String sessionId, ChatMessage msg) {
        Sinks.Many<String> sink = sessionSinks.get(sessionId);
        if (sink != null) {
            try {
                sink.tryEmitNext(objectMapper.writeValueAsString(msg));
            } catch (Exception e) {
                log.error("Error serializing response message", e);
            }
        }
    }

    // ── Handlers ─────────────────────────────────────────────────────────────

    private void handleRegister(WebSocketSession session, ChatMessage msg) {
        String phone = msg.getSender();
        if (phone == null || phone.isBlank()) return;

        String sessionId = session.getId();
        sessionToPhone.put(sessionId, phone);
        phoneToSessions.computeIfAbsent(phone, k -> new CopyOnWriteArraySet<>()).add(sessionId);

        log.info("User {} registered reactively (sessionId={})", phone, sessionId);

        sendToSession(sessionId, ChatMessage.builder()
                .sender("SERVER")
                .type("REGISTER")
                .status("SUCCESS")
                .content("Registered as " + phone)
                .timestamp(System.currentTimeMillis())
                .build());

        broadcastOnlineUsers();
    }

    private void handleJoin(WebSocketSession session, ChatMessage msg) {
        String phone   = msg.getSender();
        String groupId = msg.getGroupId();
        if (groupId == null) return;

        ChatGroup group = groupService.findGroup(groupId).orElse(null);
        boolean isMember = group != null && group.hasMember(phone);
        boolean isLeftMember = group != null && group.getLeftMembers() != null && group.getLeftMembers().contains(phone);
        if (group == null || (!isMember && !isLeftMember)) {
            sendToSession(session.getId(), errorResponse("JOIN", groupId, "You are not a member of this group!"));
            return;
        }

        groupSessions.computeIfAbsent(groupId, k -> new CopyOnWriteArraySet<>()).add(session.getId());

        if (isMember) {
            kafkaMessagingService.publishMessage(ChatMessage.builder()
                    .sender("SERVER")
                    .type("JOIN")
                    .groupId(groupId)
                    .content(phone + " joined the group!")
                    .timestamp(System.currentTimeMillis())
                    .status("SUCCESS")
                    .build());
        }

        log.info("{} joined group {}", phone, groupId);
    }

    private void handleLeave(WebSocketSession session, ChatMessage msg) {
        String phone   = msg.getSender();
        String groupId = msg.getGroupId();
        if (groupId == null) return;

        Set<String> sessions = groupSessions.get(groupId);
        if (sessions != null) {
            sessions.remove(session.getId());
        }

        kafkaMessagingService.publishMessage(ChatMessage.builder()
                .sender("SERVER")
                .type("LEAVE")
                .groupId(groupId)
                .content(phone + " left the group!")
                .timestamp(System.currentTimeMillis())
                .status("SUCCESS")
                .build());
    }

    private void handleCreateGroup(WebSocketSession session, ChatMessage msg) {
        String groupId   = UUID.randomUUID().toString();
        String groupName = msg.getContent();
        String creator   = msg.getSender();

        ChatGroup group = groupService.createGroup(groupId, groupName, creator);

        sendToSession(session.getId(), ChatMessage.builder()
                .sender("SERVER")
                .type("CREATE_GROUP")
                .groupId(groupId)
                .content("Group created: " + groupName)
                .timestamp(System.currentTimeMillis())
                .status("SUCCESS")
                .data(group)
                .build());

        log.info("Group '{}' ({}) created by {}", groupName, groupId, creator);
    }

    private void handleGroupText(WebSocketSession session, ChatMessage msg) {
        String groupId = msg.getGroupId();
        String sender = msg.getSender();
        ChatGroup group = groupService.findGroup(groupId).orElse(null);
        if (group == null || !group.hasMember(sender)) {
            sendToSession(session.getId(), errorResponse("TEXT", groupId, "You cannot send messages to this group as you are not an active member."));
            return;
        }

        msg.setTimestamp(System.currentTimeMillis());
        msg.setMessageId(UUID.randomUUID().toString());

        messageRepository.save(MessageEntity.builder()
                .messageId(msg.getMessageId())
                .sender(msg.getSender())
                .groupId(msg.getGroupId())
                .content(msg.getContent())
                .type(msg.getType())
                .timestamp(msg.getTimestamp())
                .build());

        // ── Direct local delivery (zero-latency, no Kafka round-trip needed) ──
        // Mark as locally delivered so the Kafka consumer doesn't re-deliver it.
        locallyDeliveredIds.add(msg.getMessageId());
        evictionScheduler.schedule(
                () -> locallyDeliveredIds.remove(msg.getMessageId()),
                30, TimeUnit.SECONDS);

        // Deliver immediately to every active member's WebSocket sinks on this server
        final ChatMessage finalMsg = msg;
        group.getMembers().forEach(phone -> deliverToUser(phone, finalMsg));

        // Also publish to Kafka for cross-server fanout / audit trail
        kafkaMessagingService.publishMessage(msg);
    }

    private void handleDirectMessage(WebSocketSession session, ChatMessage msg) {
        String recipientPhone = msg.getRecipientId();
        if (recipientPhone == null || recipientPhone.isBlank()) {
            log.warn("DIRECT_MESSAGE missing recipientId from {}", msg.getSender());
            return;
        }

        msg.setTimestamp(System.currentTimeMillis());
        msg.setMessageId(UUID.randomUUID().toString());

        messageRepository.save(MessageEntity.builder()
                .messageId(msg.getMessageId())
                .sender(msg.getSender())
                .recipientId(recipientPhone)
                .content(msg.getContent())
                .type(msg.getType())
                .timestamp(msg.getTimestamp())
                .build());

        // ── Direct local delivery (zero-latency) ──────────────────────────────
        locallyDeliveredIds.add(msg.getMessageId());
        evictionScheduler.schedule(
                () -> locallyDeliveredIds.remove(msg.getMessageId()),
                30, TimeUnit.SECONDS);

        // Deliver to recipient and echo back to sender immediately
        deliverToUser(recipientPhone, msg);
        deliverToUser(msg.getSender(), msg);

        // Also publish to Kafka for cross-server fanout / audit trail
        kafkaMessagingService.publishMessage(msg);
    }

    private void handleListGroups(WebSocketSession session) {
        String phone = sessionToPhone.get(session.getId());
        List<ChatGroup> memberGroups = new ArrayList<>();
        if (phone != null) {
            memberGroups = groupService.getAllGroups().stream()
                    .filter(g -> g.hasMember(phone) || (g.getLeftMembers() != null && g.getLeftMembers().contains(phone)))
                    .toList();
        }
        sendToSession(session.getId(), ChatMessage.builder()
                .sender("SERVER")
                .type("LIST_GROUPS")
                .timestamp(System.currentTimeMillis())
                .status("SUCCESS")
                .data(memberGroups)
                .build());
    }

    private void handleListUsers(WebSocketSession session, ChatMessage msg) {
        String groupId = msg.getGroupId();
        ChatGroup group = groupId != null ? groupService.findGroup(groupId).orElse(null) : null;
        Set<String> members = group != null ? group.getMembers() : new HashSet<>();

        List<Map<String, String>> memberDetails = new ArrayList<>();
        for (String phone : members) {
            Map<String, String> detail = new HashMap<>();
            detail.put("phoneNumber", phone);
            userRepository.findByPhoneNumber(phone).ifPresentOrElse(
                    user -> detail.put("displayName", user.getDisplayName()),
                    ()   -> detail.put("displayName", phone)
            );

            boolean isAdmin = group != null && group.isAdmin(phone);
            boolean isCreator = group != null && phone.equals(group.getCreator());
            detail.put("isAdmin", String.valueOf(isAdmin));
            detail.put("isCreator", String.valueOf(isCreator));

            memberDetails.add(detail);
        }

        sendToSession(session.getId(), ChatMessage.builder()
                .sender("SERVER")
                .type("LIST_USERS")
                .groupId(groupId)
                .timestamp(System.currentTimeMillis())
                .status("SUCCESS")
                .data(memberDetails)
                .build());
    }

    private void handleListOnlineUsers(WebSocketSession session) {
        sendToSession(session.getId(), ChatMessage.builder()
                .sender("SERVER")
                .type("LIST_ONLINE_USERS")
                .timestamp(System.currentTimeMillis())
                .status("SUCCESS")
                .data(new ArrayList<>(phoneToSessions.keySet()))
                .build());
    }

    private void handleDeleteGroup(WebSocketSession session, ChatMessage msg) {
        String groupId = msg.getGroupId();
        String sender  = msg.getSender();
        if (groupId == null) return;

        ChatGroup group = groupService.findGroup(groupId).orElse(null);
        if (group == null) return;

        if (!group.getCreator().equals(sender)) {
            sendToSession(session.getId(), errorResponse("DELETE_GROUP", groupId, "Only the primary group admin can delete the group!"));
            return;
        }

        messageRepository.deleteByGroupId(groupId);
        groupService.deleteGroup(groupId);

        ChatMessage deleteMsg = ChatMessage.builder()
                .sender("SERVER")
                .type("DELETE_GROUP")
                .groupId(groupId)
                .timestamp(System.currentTimeMillis())
                .status("SUCCESS")
                .content("Group has been deleted by the primary admin.")
                .build();

        kafkaMessagingService.publishMessage(deleteMsg);
        log.info("Group {} deleted by primary admin {}", groupId, sender);
    }

    private void handleAddGroupMember(WebSocketSession session, ChatMessage msg) {
        String groupId     = msg.getGroupId();
        String memberPhone = msg.getContent();
        String sender      = msg.getSender();
        if (groupId == null || memberPhone == null || memberPhone.isBlank()) return;

        ChatGroup group = groupService.findGroup(groupId).orElse(null);
        if (group == null) return;

        boolean isSenderCreator = group.getCreator().equals(sender);
        boolean isSenderAdmin   = group.isAdmin(sender);
        if (!isSenderCreator && !isSenderAdmin) {
            sendToSession(session.getId(), errorResponse("ADD_GROUP_MEMBER", groupId, "Only group admins or the creator can add members!"));
            return;
        }

        if (!userRepository.existsByPhoneNumber(memberPhone)) {
            sendToSession(session.getId(), errorResponse("ADD_GROUP_MEMBER", groupId,
                    "User with phone number " + memberPhone + " does not exist!"));
            return;
        }

        groupService.addMemberToGroup(groupId, memberPhone);

        kafkaMessagingService.publishMessage(ChatMessage.builder()
                .sender("SERVER")
                .type("JOIN")
                .groupId(groupId)
                .content(memberPhone + " was added to the group!")
                .timestamp(System.currentTimeMillis())
                .status("SUCCESS")
                .build());

        // Notify the newly added user
        kafkaMessagingService.publishMessage(ChatMessage.builder()
                .sender("SERVER")
                .type("GROUP_INVITATION")
                .groupId(groupId)
                .recipientId(memberPhone)
                .content(group.getGroupName())
                .timestamp(System.currentTimeMillis())
                .status("SUCCESS")
                .build());

        sendToSession(session.getId(), ChatMessage.builder()
                .sender("SERVER")
                .type("ADD_GROUP_MEMBER")
                .groupId(groupId)
                .timestamp(System.currentTimeMillis())
                .status("SUCCESS")
                .content("Member added successfully!")
                .build());
    }

    private void handleRemoveGroupMember(WebSocketSession session, ChatMessage msg) {
        String groupId     = msg.getGroupId();
        String memberPhone = msg.getContent();
        String sender      = msg.getSender();
        if (groupId == null || memberPhone == null || memberPhone.isBlank()) return;

        ChatGroup group = groupService.findGroup(groupId).orElse(null);
        if (group == null) return;

        boolean isSenderCreator = group.getCreator().equals(sender);
        boolean isSenderAdmin   = group.isAdmin(sender);
        boolean isTargetAdmin   = group.isAdmin(memberPhone);
        boolean isSelfLeave     = memberPhone.equals(sender);

        boolean allowed = isSelfLeave || isSenderCreator || (isSenderAdmin && !isTargetAdmin);
        if (!allowed) {
            sendToSession(session.getId(), errorResponse("REMOVE_GROUP_MEMBER", groupId, "You do not have permission to remove this member!"));
            return;
        }

        groupService.removeMemberFromGroup(groupId, memberPhone);

        String leaveMessage = isSelfLeave
                ? (memberPhone + " left the group!")
                : (memberPhone + " was removed from the group!");

        kafkaMessagingService.publishMessage(ChatMessage.builder()
                .sender("SERVER")
                .type("LEAVE")
                .groupId(groupId)
                .content(leaveMessage)
                .timestamp(System.currentTimeMillis())
                .status("SUCCESS")
                .build());

        if (!isSelfLeave) {
            kafkaMessagingService.publishMessage(ChatMessage.builder()
                    .sender("SERVER")
                    .type("GROUP_KICK")
                    .groupId(groupId)
                    .recipientId(memberPhone)
                    .content(group.getGroupName())
                    .timestamp(System.currentTimeMillis())
                    .status("SUCCESS")
                    .build());
        }

        sendToSession(session.getId(), ChatMessage.builder()
                .sender("SERVER")
                .type("REMOVE_GROUP_MEMBER")
                .groupId(groupId)
                .timestamp(System.currentTimeMillis())
                .status("SUCCESS")
                .content("Member removed successfully!")
                .build());
    }

    private void handlePromoteMember(WebSocketSession session, ChatMessage msg) {
        String groupId     = msg.getGroupId();
        String memberPhone = msg.getContent();
        String sender      = msg.getSender();
        if (groupId == null || memberPhone == null || memberPhone.isBlank()) return;

        ChatGroup group = groupService.findGroup(groupId).orElse(null);
        if (group == null) return;

        boolean isCreator = group.getCreator().equals(sender);
        if (!isCreator) {
            sendToSession(session.getId(), errorResponse("PROMOTE_MEMBER", groupId, "Only the group owner can promote members to admin!"));
            return;
        }

        groupService.promoteToAdmin(groupId, memberPhone);

        kafkaMessagingService.publishMessage(ChatMessage.builder()
                .sender("SERVER")
                .type("PROMOTE_MEMBER")
                .groupId(groupId)
                .content(memberPhone)
                .timestamp(System.currentTimeMillis())
                .status("SUCCESS")
                .build());
    }

    private void handleDemoteMember(WebSocketSession session, ChatMessage msg) {
        String groupId     = msg.getGroupId();
        String memberPhone = msg.getContent();
        String sender      = msg.getSender();
        if (groupId == null || memberPhone == null || memberPhone.isBlank()) return;

        ChatGroup group = groupService.findGroup(groupId).orElse(null);
        if (group == null) return;

        boolean isCreator = group.getCreator().equals(sender);
        if (!isCreator) {
            sendToSession(session.getId(), errorResponse("DEMOTE_MEMBER", groupId, "Only the group owner can demote admins!"));
            return;
        }

        if (memberPhone.equals(group.getCreator())) {
            sendToSession(session.getId(), errorResponse("DEMOTE_MEMBER", groupId, "Cannot demote the primary group creator!"));
            return;
        }

        groupService.demoteToNormalUser(groupId, memberPhone);

        kafkaMessagingService.publishMessage(ChatMessage.builder()
                .sender("SERVER")
                .type("DEMOTE_MEMBER")
                .groupId(groupId)
                .content(memberPhone)
                .timestamp(System.currentTimeMillis())
                .status("SUCCESS")
                .build());
    }

    private void handleDeleteDm(WebSocketSession session, ChatMessage msg) {
        String sender    = msg.getSender();
        String recipient = msg.getRecipientId();
        if (recipient == null || recipient.isBlank()) return;

        contactRepository.deleteByUserPhoneAndContactPhone(sender, recipient);

        List<MessageEntity> messages = messageRepository.findDmHistoryAll(sender, recipient);
        List<MessageEntity> toSave = new ArrayList<>();
        List<MessageEntity> toDelete = new ArrayList<>();

        for (MessageEntity m : messages) {
            if (sender.equals(m.getSender())) {
                m.setDeletedBySender(true);
            } else if (sender.equals(m.getRecipientId())) {
                m.setDeletedByRecipient(true);
            }

            if (m.isDeletedBySender() && m.isDeletedByRecipient()) {
                toDelete.add(m);
            } else {
                toSave.add(m);
            }
        }

        if (!toSave.isEmpty()) {
            messageRepository.saveAll(toSave);
        }
        if (!toDelete.isEmpty()) {
            messageRepository.deleteAll(toDelete);
        }

        sendToSession(session.getId(), ChatMessage.builder()
                .sender("SERVER")
                .type("DELETE_DM")
                .recipientId(recipient)
                .timestamp(System.currentTimeMillis())
                .status("SUCCESS")
                .content("Chat deleted successfully!")
                .build());
    }

    // ── Kafka Route Handler ──────────────────────────────────────────────────

    public void routeMessageFromKafka(ChatMessage msg) {
        log.debug("Routing message from Kafka: type={} sender={}", msg.getType(), msg.getSender());
        String type = msg.getType();

        // Skip messages already delivered locally to avoid duplicates.
        // (Local delivery happens synchronously in the handler before Kafka publish.)
        if (msg.getMessageId() != null && locallyDeliveredIds.contains(msg.getMessageId())) {
            log.debug("Skipping Kafka re-delivery for locally-delivered messageId={}", msg.getMessageId());
            return;
        }

        if ("DIRECT_MESSAGE".equals(type)) {
            // Deliver to recipient
            deliverToUser(msg.getRecipientId(), msg);
            // Deliver back to sender (echo)
            deliverToUser(msg.getSender(), msg);
        } else if ("TEXT".equals(type)) {
            // Group chat message: deliver to all active members of the group
            ChatGroup group = groupService.findGroup(msg.getGroupId()).orElse(null);
            if (group != null) {
                group.getMembers().forEach(phone -> deliverToUser(phone, msg));
            }
        } else if ("JOIN".equals(type) || "LEAVE".equals(type) || "PROMOTE_MEMBER".equals(type) || "DEMOTE_MEMBER".equals(type)) {
            // Group lifecycle updates: broadcast to everyone currently active in that group room on this server
            Set<String> sessionIds = groupSessions.get(msg.getGroupId());
            if (sessionIds != null) {
                sessionIds.forEach(sessId -> sendToSession(sessId, msg));
            }
        } else if ("GROUP_INVITATION".equals(type)) {
            // invitation: deliver only to the added user (the content field contains the group name, we need to extract phone if needed,
            // wait: handleAddGroupMember sends GROUP_INVITATION with groupName as content, but where is the targetPhone?
            // Ah! In handleAddGroupMember, it sends GROUP_INVITATION to targetSession (the added user's session).
            // When going through Kafka, we should make sure we specify the target recipient!
            // Let's check how the invite message is constructed. In handleAddGroupMember, we did:
            // ChatMessage.builder().type("GROUP_INVITATION").groupId(groupId).content(groupName)...
            // Wait, we didn't specify recipientId!
            // Let's look: when handleAddGroupMember publishes GROUP_INVITATION to Kafka, it should set `recipientId(memberPhone)`!
            // Let's check: in `handleAddGroupMember`, we did `kafkaMessagingService.publishMessage(...)`.
            // Let's modify the handler below to route GROUP_INVITATION to `msg.getRecipientId()`.
            deliverToUser(msg.getRecipientId(), msg);
        } else if ("GROUP_KICK".equals(type)) {
            // kick: deliver only to the kicked user
            deliverToUser(msg.getRecipientId(), msg);
        } else if ("DELETE_GROUP".equals(type)) {
            // Group deleted: notify all members (active and left) so they clear it
            ChatGroup group = groupService.findGroup(msg.getGroupId()).orElse(null);
            if (group != null) {
                group.getMembers().forEach(phone -> deliverToUser(phone, msg));
                if (group.getLeftMembers() != null) {
                    group.getLeftMembers().forEach(phone -> deliverToUser(phone, msg));
                }
            }
        }
    }

    private void deliverToUser(String phone, ChatMessage msg) {
        if (phone == null) return;
        Set<String> sessionIds = phoneToSessions.get(phone);
        if (sessionIds != null) {
            sessionIds.forEach(sessId -> sendToSession(sessId, msg));
        }
    }

    private void broadcastOnlineUsers() {
        ChatMessage msg = ChatMessage.builder()
                .sender("SERVER")
                .type("LIST_ONLINE_USERS")
                .timestamp(System.currentTimeMillis())
                .status("SUCCESS")
                .data(new ArrayList<>(phoneToSessions.keySet()))
                .build();

        // Broadcast to all active sessions on this server
        sessionSinks.keySet().forEach(sessId -> sendToSession(sessId, msg));
    }

    private ChatMessage errorResponse(String type, String groupId, String content) {
        return ChatMessage.builder()
                .sender("SERVER")
                .type(type)
                .groupId(groupId)
                .timestamp(System.currentTimeMillis())
                .status("ERROR")
                .content(content)
                .build();
    }
}
