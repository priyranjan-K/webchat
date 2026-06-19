package au.com.example.webchat.server.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "messages")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MessageEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String messageId;

    @Column(nullable = false)
    private String sender;

    @Column
    private String recipientId;

    @Column
    private String groupId;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(nullable = false)
    private String type;

    @Column(nullable = false)
    private long timestamp;

    @Column(nullable = false)
    @Builder.Default
    private boolean deletedBySender = false;

    @Column(nullable = false)
    @Builder.Default
    private boolean deletedByRecipient = false;
}
