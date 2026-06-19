package au.com.example.webchat.server.websocket;

import au.com.example.webchat.server.dto.ChatMessage;
import au.com.example.webchat.server.model.MessageEntity;
import au.com.example.webchat.server.repository.ContactRepository;
import au.com.example.webchat.server.repository.MessageRepository;
import au.com.example.webchat.server.repository.UserRepository;
import au.com.example.webchat.server.service.GroupService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Central WebSocket message handler for the WebChat application.
 * <p>
 * Manages two in-memory structures:
 * <ul>
 *   <li>{@code onlineUsers}  — phone → WebSocket session for every registered user</li>
 *   <li>{@code userSessions} — "phone:groupId" → {@link UserSession} for group broadcasting</li>
 * </ul>
 *
 * <p>Supported message types:
 * <pre>
 *   REGISTER          — links a phone number to this session; broadcasts online roster
 *   JOIN              — join a group room
 *   LEAVE             — leave a group room
 *   CREATE_GROUP      — create a named group
 *   TEXT              — send text to a group
 *   DIRECT_MESSAGE    — send a private message to a specific user
 *   LIST_GROUPS       — list all active groups
 *   LIST_USERS        — list members of a specific group
 *   LIST_ONLINE_USERS — list all currently online users
 *   DELETE_GROUP      — delete a group (creator only)
 *   ADD_GROUP_MEMBER  — add a user to a group
 *   REMOVE_GROUP_MEMBER — remove a user from a group
 *   DELETE_DM         — delete the DM history between two users
 * </pre>
 */
@Component
@Slf4j
public class ChatWebSocketHandler extends TextWebSocketHandler {

    private final ObjectMapper      objectMapper = new ObjectMapper();
    private final GroupService      groupService;
    private final MessageRepository messageRepository;
    private final UserRepository    userRepository;
    private final ContactRepository contactRepository;

    /** phone → WebSocket session (all connected users, registered after login). */
    private final Map<String, WebSocketSession> onlineUsers  = new ConcurrentHashMap<>();

    /** "phone:groupId" → {@link UserSession} (for group broadcasting). */
    private final Map<String, UserSession>       userSessions = new ConcurrentHashMap<>();

    public ChatWebSocketHandler(
            GroupService      groupService,
            MessageRepository messageRepository,
            UserRepository    userRepository,
            ContactRepository contactRepository) {
        this.groupService      = groupService;
        this.messageRepository = messageRepository;
        this.userRepository    = userRepository;
        this.contactRepository = contactRepository;
    }

    // ── Connection lifecycle ──────────────────────────────────────────────────

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        log.info("WebSocket connection opened: sessionId={}", session.getId());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        String phone = phoneBySessionId(session.getId());
        if (phone != null) {
            onlineUsers.remove(phone);
            broadcastOnlineUsers();
            log.info("User {} went offline", phone);
        }

        // Clean up all group memberships for this session
        final String displayPhone = phone != null ? phone : "Someone";
        userSessions.entrySet().removeIf(entry -> {
            if (!entry.getValue().getSession().getId().equals(session.getId())) return false;

            String groupId = entry.getValue().getGroupId();
            try {
                broadcastToGroup(groupId, ChatMessage.builder()
                        .sender("SERVER")
                        .type("LEAVE")
                        .groupId(groupId)
                        .content(displayPhone + " disconnected!")
                        .timestamp(System.currentTimeMillis())
                        .status("SUCCESS")
                        .build());
            } catch (IOException ex) {
                log.error("Error broadcasting disconnect for group {}", groupId, ex);
            }
            return true;
        });
    }

    // ── Message dispatch ──────────────────────────────────────────────────────

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        try {
            ChatMessage msg = objectMapper.readValue(message.getPayload(), ChatMessage.class);
            log.debug("Message received: type={} sender={}", msg.getType(), msg.getSender());

            switch (msg.getType()) {
                case "REGISTER"            -> handleRegister(session, msg);
                case "JOIN"                -> handleJoin(session, msg);
                case "LEAVE"               -> handleLeave(msg);
                case "CREATE_GROUP"        -> handleCreateGroup(session, msg);
                case "TEXT"                -> handleGroupText(msg);
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
    }

    // ── Handlers ─────────────────────────────────────────────────────────────

    /**
     * REGISTER — links the sender's phone number to this session.
     * Broadcasts an updated online roster to all connected users.
     */
    private void handleRegister(WebSocketSession session, ChatMessage msg) throws IOException {
        String phone = msg.getSender();
        if (phone == null || phone.isBlank()) return;

        onlineUsers.put(phone, session);
        log.info("User {} registered (online)", phone);

        send(session, ChatMessage.builder()
                .sender("SERVER")
                .type("REGISTER")
                .status("SUCCESS")
                .content("Registered as " + phone)
                .timestamp(System.currentTimeMillis())
                .build());

        broadcastOnlineUsers();
    }

    /** JOIN — user joins a specific group room. */
    private void handleJoin(WebSocketSession session, ChatMessage msg) throws IOException {
        String phone   = msg.getSender();
        String groupId = msg.getGroupId();
        if (groupId == null) return;

        ChatGroup group = groupService.findGroup(groupId).orElse(null);
        if (group == null || !group.hasMember(phone)) {
            send(session, errorResponse("JOIN", groupId, "You are not a member of this group!"));
            return;
        }

        UserSession us = UserSession.builder()
                .session(session)
                .username(phone)
                .groupId(groupId)
                .joinedAt(System.currentTimeMillis())
                .build();
        userSessions.put(phone + ":" + groupId, us);

        broadcastToGroup(groupId, ChatMessage.builder()
                .sender("SERVER")
                .type("JOIN")
                .groupId(groupId)
                .content(phone + " joined the group!")
                .timestamp(System.currentTimeMillis())
                .status("SUCCESS")
                .build());

        log.info("{} joined group {}", phone, groupId);
    }

    /** LEAVE — user leaves a group room (remains a member in GroupService). */
    private void handleLeave(ChatMessage msg) throws IOException {
        String phone   = msg.getSender();
        String groupId = msg.getGroupId();
        if (groupId == null) return;

        userSessions.remove(phone + ":" + groupId);

        broadcastToGroup(groupId, ChatMessage.builder()
                .sender("SERVER")
                .type("LEAVE")
                .groupId(groupId)
                .content(phone + " left the group!")
                .timestamp(System.currentTimeMillis())
                .status("SUCCESS")
                .build());
    }

    /** CREATE_GROUP — creates a new named group and informs the creator. */
    private void handleCreateGroup(WebSocketSession session, ChatMessage msg) throws IOException {
        String groupId   = UUID.randomUUID().toString();
        String groupName = msg.getContent();
        String creator   = msg.getSender();

        ChatGroup group = groupService.createGroup(groupId, groupName, creator);

        send(session, ChatMessage.builder()
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

    /** TEXT — broadcasts a message to all group room participants and persists it. */
    private void handleGroupText(ChatMessage msg) throws IOException {
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

        broadcastToGroup(msg.getGroupId(), msg);
    }

    /**
     * DIRECT_MESSAGE — sends a private message to a specific user.
     * Both sender and recipient receive the message so multi-tab scenarios work.
     * The message is persisted for offline delivery.
     */
    private void handleDirectMessage(WebSocketSession senderSession, ChatMessage msg) throws IOException {
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

        WebSocketSession recipientSession = onlineUsers.get(recipientPhone);
        if (recipientSession != null && recipientSession.isOpen()) {
            send(recipientSession, msg);
            log.debug("DM delivered from {} to {}", msg.getSender(), recipientPhone);
        } else {
            log.info("Recipient {} is offline — DM saved to DB for later delivery", recipientPhone);
        }

        // Echo to sender so their own UI reflects the sent message
        send(senderSession, msg);
    }

    /** LIST_GROUPS — returns only groups where the user is a member. */
    private void handleListGroups(WebSocketSession session) throws IOException {
        String phone = phoneBySessionId(session.getId());
        List<ChatGroup> memberGroups = new ArrayList<>();
        if (phone != null) {
            memberGroups = groupService.getAllGroups().stream()
                    .filter(g -> g.hasMember(phone))
                    .toList();
        }
        send(session, ChatMessage.builder()
                .sender("SERVER")
                .type("LIST_GROUPS")
                .timestamp(System.currentTimeMillis())
                .status("SUCCESS")
                .data(memberGroups)
                .build());
    }

    /** LIST_USERS — returns group members with display names resolved from the database. */
    private void handleListUsers(WebSocketSession session, ChatMessage msg) throws IOException {
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

        send(session, ChatMessage.builder()
                .sender("SERVER")
                .type("LIST_USERS")
                .groupId(groupId)
                .timestamp(System.currentTimeMillis())
                .status("SUCCESS")
                .data(memberDetails)
                .build());
    }

    /** LIST_ONLINE_USERS — returns the set of currently registered (online) phone numbers. */
    private void handleListOnlineUsers(WebSocketSession session) throws IOException {
        send(session, ChatMessage.builder()
                .sender("SERVER")
                .type("LIST_ONLINE_USERS")
                .timestamp(System.currentTimeMillis())
                .status("SUCCESS")
                .data(new ArrayList<>(onlineUsers.keySet()))
                .build());
    }

    /** DELETE_GROUP — removes a group (creator only). Notifies all group members. */
    private void handleDeleteGroup(WebSocketSession session, ChatMessage msg) throws IOException {
        String groupId = msg.getGroupId();
        String sender  = msg.getSender();
        if (groupId == null) return;

        ChatGroup group = groupService.findGroup(groupId).orElse(null);
        if (group == null) return;

        if (!group.getCreator().equals(sender)) {
            send(session, errorResponse("DELETE_GROUP", groupId, "Only the primary group admin can delete the group!"));
            return;
        }

        Set<String> groupMembers = new HashSet<>(group.getMembers());
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

        broadcastToGroup(groupId, deleteMsg);

        // Also notify all online group members so the group disappears from their sidebar in real-time
        for (String memberPhone : groupMembers) {
            WebSocketSession memberSession = onlineUsers.get(memberPhone);
            if (memberSession != null && memberSession.isOpen()) {
                send(memberSession, deleteMsg);
            }
        }

        log.info("Group {} deleted by primary admin {}", groupId, sender);
    }

    /** ADD_GROUP_MEMBER — adds a user to the group (verifies user exists first). */
    private void handleAddGroupMember(WebSocketSession session, ChatMessage msg) throws IOException {
        String groupId     = msg.getGroupId();
        String memberPhone = msg.getContent();
        if (groupId == null || memberPhone == null || memberPhone.isBlank()) return;

        ChatGroup group = groupService.findGroup(groupId).orElse(null);
        if (group == null) return;

        if (!userRepository.existsByPhoneNumber(memberPhone)) {
            send(session, errorResponse("ADD_GROUP_MEMBER", groupId,
                    "User with phone number " + memberPhone + " does not exist!"));
            return;
        }

        groupService.addMemberToGroup(groupId, memberPhone);

        broadcastToGroup(groupId, ChatMessage.builder()
                .sender("SERVER")
                .type("JOIN")
                .groupId(groupId)
                .content(memberPhone + " was added to the group!")
                .timestamp(System.currentTimeMillis())
                .status("SUCCESS")
                .build());

        // Notify the newly added user if they are online
        WebSocketSession targetSession = onlineUsers.get(memberPhone);
        if (targetSession != null && targetSession.isOpen()) {
            send(targetSession, ChatMessage.builder()
                    .sender("SERVER")
                    .type("GROUP_INVITATION")
                    .groupId(groupId)
                    .content(group.getGroupName())
                    .timestamp(System.currentTimeMillis())
                    .status("SUCCESS")
                    .build());
        }

        send(session, ChatMessage.builder()
                .sender("SERVER")
                .type("ADD_GROUP_MEMBER")
                .groupId(groupId)
                .timestamp(System.currentTimeMillis())
                .status("SUCCESS")
                .content("Member added successfully!")
                .build());

        log.info("User {} added {} to group {}", msg.getSender(), memberPhone, groupId);
    }

    /** REMOVE_GROUP_MEMBER — removes a user from the group (admins or self only). */
    private void handleRemoveGroupMember(WebSocketSession session, ChatMessage msg) throws IOException {
        String groupId     = msg.getGroupId();
        String memberPhone = msg.getContent();
        String sender      = msg.getSender();
        if (groupId == null || memberPhone == null || memberPhone.isBlank()) return;

        ChatGroup group = groupService.findGroup(groupId).orElse(null);
        if (group == null) return;

        boolean isSenderCreator = group.getCreator().equals(sender);
        boolean isSenderAdmin   = group.isAdmin(sender);
        boolean isTargetCreator = group.getCreator().equals(memberPhone);
        boolean isTargetAdmin   = group.isAdmin(memberPhone);
        boolean isSelfLeave     = memberPhone.equals(sender);

        boolean allowed = isSelfLeave || isSenderCreator || (isSenderAdmin && !isTargetAdmin);
        if (!allowed) {
            send(session, errorResponse("REMOVE_GROUP_MEMBER", groupId, "You do not have permission to remove this member!"));
            return;
        }

        groupService.removeMemberFromGroup(groupId, memberPhone);

        String leaveMessage = isSelfLeave
                ? (memberPhone + " left the group!")
                : (memberPhone + " was removed from the group!");

        broadcastToGroup(groupId, ChatMessage.builder()
                .sender("SERVER")
                .type("LEAVE")
                .groupId(groupId)
                .content(leaveMessage)
                .timestamp(System.currentTimeMillis())
                .status("SUCCESS")
                .build());

        // Notify the removed user if they are online and it wasn't self-leave
        if (!isSelfLeave) {
            WebSocketSession targetSession = onlineUsers.get(memberPhone);
            if (targetSession != null && targetSession.isOpen()) {
                send(targetSession, ChatMessage.builder()
                        .sender("SERVER")
                        .type("GROUP_KICK")
                        .groupId(groupId)
                        .content(group.getGroupName())
                        .timestamp(System.currentTimeMillis())
                        .status("SUCCESS")
                        .build());
            }
        }

        send(session, ChatMessage.builder()
                .sender("SERVER")
                .type("REMOVE_GROUP_MEMBER")
                .groupId(groupId)
                .timestamp(System.currentTimeMillis())
                .status("SUCCESS")
                .content("Member removed successfully!")
                .build());

        log.info("User {} removed/left {} from group {}", sender, memberPhone, groupId);
    }

    /** PROMOTE_MEMBER — promotes a member to admin. Only the group creator/owner can do this. */
    private void handlePromoteMember(WebSocketSession session, ChatMessage msg) throws IOException {
        String groupId     = msg.getGroupId();
        String memberPhone = msg.getContent();
        String sender      = msg.getSender();
        if (groupId == null || memberPhone == null || memberPhone.isBlank()) return;

        ChatGroup group = groupService.findGroup(groupId).orElse(null);
        if (group == null) return;

        // Only the 1st creator (owner) can promote members
        boolean isCreator = group.getCreator().equals(sender);
        if (!isCreator) {
            send(session, errorResponse("PROMOTE_MEMBER", groupId, "Only the group owner can promote members to admin!"));
            return;
        }

        group.promoteToAdmin(memberPhone);

        // Broadcast change to the group so everyone's UI updates
        broadcastToGroup(groupId, ChatMessage.builder()
                .sender("SERVER")
                .type("PROMOTE_MEMBER")
                .groupId(groupId)
                .content(memberPhone)
                .timestamp(System.currentTimeMillis())
                .status("SUCCESS")
                .build());

        log.info("User {} promoted {} to admin in group {}", sender, memberPhone, groupId);
    }

    /** DEMOTE_MEMBER — demotes an admin back to a normal user. Only the group creator/owner can do this. */
    private void handleDemoteMember(WebSocketSession session, ChatMessage msg) throws IOException {
        String groupId     = msg.getGroupId();
        String memberPhone = msg.getContent();
        String sender      = msg.getSender();
        if (groupId == null || memberPhone == null || memberPhone.isBlank()) return;

        ChatGroup group = groupService.findGroup(groupId).orElse(null);
        if (group == null) return;

        // Only the 1st creator (owner) can demote members
        boolean isCreator = group.getCreator().equals(sender);
        if (!isCreator) {
            send(session, errorResponse("DEMOTE_MEMBER", groupId, "Only the group owner can demote admins!"));
            return;
        }

        if (memberPhone.equals(group.getCreator())) {
            send(session, errorResponse("DEMOTE_MEMBER", groupId, "Cannot demote the primary group creator!"));
            return;
        }

        group.demoteToNormalUser(memberPhone);

        // Broadcast change to the group
        broadcastToGroup(groupId, ChatMessage.builder()
                .sender("SERVER")
                .type("DEMOTE_MEMBER")
                .groupId(groupId)
                .content(memberPhone)
                .timestamp(System.currentTimeMillis())
                .status("SUCCESS")
                .build());

        log.info("User {} demoted admin {} in group {}", sender, memberPhone, groupId);
    }

    /** DELETE_DM — deletes the contact relationship and marks DM history as deleted for this user only. */
    private void handleDeleteDm(WebSocketSession session, ChatMessage msg) throws IOException {
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

        send(session, ChatMessage.builder()
                .sender("SERVER")
                .type("DELETE_DM")
                .recipientId(recipient)
                .timestamp(System.currentTimeMillis())
                .status("SUCCESS")
                .content("Chat deleted successfully!")
                .build());

        log.info("DM history between {} and {} deleted by {}", sender, recipient, sender);
    }

    // ── Transport helpers ─────────────────────────────────────────────────────

    /** Sends a {@link ChatMessage} to a single session. Thread-safe via session-level lock. */
    private void send(WebSocketSession session, ChatMessage msg) throws IOException {
        synchronized (session) {
            if (session.isOpen()) {
                session.sendMessage(new TextMessage(objectMapper.writeValueAsString(msg)));
            }
        }
    }

    /** Broadcasts a {@link ChatMessage} to all sessions currently in the given group room. */
    private void broadcastToGroup(String groupId, ChatMessage msg) throws IOException {
        String       json  = objectMapper.writeValueAsString(msg);
        TextMessage  frame = new TextMessage(json);
        for (UserSession us : userSessions.values()) {
            if (!groupId.equals(us.getGroupId())) continue;
            WebSocketSession s = us.getSession();
            synchronized (s) {
                if (s.isOpen()) s.sendMessage(frame);
            }
        }
    }

    /** Broadcasts the current online user list to ALL registered sessions. */
    private void broadcastOnlineUsers() {
        try {
            ChatMessage msg = ChatMessage.builder()
                    .sender("SERVER")
                    .type("LIST_ONLINE_USERS")
                    .timestamp(System.currentTimeMillis())
                    .status("SUCCESS")
                    .data(new ArrayList<>(onlineUsers.keySet()))
                    .build();
            String      json  = objectMapper.writeValueAsString(msg);
            TextMessage frame = new TextMessage(json);
            for (WebSocketSession session : onlineUsers.values()) {
                synchronized (session) {
                    if (session.isOpen()) session.sendMessage(frame);
                }
            }
        } catch (IOException e) {
            log.error("Error broadcasting online users", e);
        }
    }

    /** Builds a standard error response {@link ChatMessage}. */
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

    /** Reverse-lookup: finds the phone number associated with a given session ID. */
    private String phoneBySessionId(String sessionId) {
        return onlineUsers.entrySet().stream()
                .filter(e -> e.getValue().getId().equals(sessionId))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse(null);
    }
}
