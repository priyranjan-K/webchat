package au.com.example.webchat.server.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "chat_groups")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GroupEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String groupId;

    @Column(nullable = false)
    private String groupName;

    @Column(nullable = false)
    private String creator;

    @Column(nullable = false)
    private Long createdAt;
}
