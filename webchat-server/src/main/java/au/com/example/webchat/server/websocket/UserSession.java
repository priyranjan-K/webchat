package au.com.example.webchat.server.websocket;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.socket.WebSocketSession;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserSession {
    private WebSocketSession session;
    private String username;
    private String groupId;
    private long joinedAt;
}
