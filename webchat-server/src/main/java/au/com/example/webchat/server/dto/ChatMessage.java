package au.com.example.webchat.server.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatMessage {
    private String sender;
    private String content;
    private String groupId;
    private String recipientId;   // for DIRECT_MESSAGE — target user's phone number
    private String type;          // TEXT, JOIN, LEAVE, CREATE_GROUP, LIST_GROUPS, LIST_USERS,
                                  // REGISTER, DIRECT_MESSAGE, LIST_ONLINE_USERS, USER_OFFLINE
    private long   timestamp;
    private String messageId;
    private String status;        // SUCCESS, ERROR
    private Object data;
}
