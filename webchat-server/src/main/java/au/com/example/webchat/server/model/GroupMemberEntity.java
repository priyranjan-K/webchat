package au.com.example.webchat.server.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "group_members", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"groupId", "phoneNumber"})
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GroupMemberEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String groupId;

    @Column(nullable = false)
    private String phoneNumber;

    @Column(nullable = false)
    @Builder.Default
    private Boolean isAdmin = false;

    @Column(nullable = false)
    @Builder.Default
    private Boolean isCreator = false;

    @Column(nullable = false)
    @Builder.Default
    private Boolean hasLeft = false;
}
