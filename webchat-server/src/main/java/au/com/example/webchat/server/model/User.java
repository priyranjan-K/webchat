package au.com.example.webchat.server.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String phoneNumber;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String displayName;

    @Column
    private String profilePictureUrl;

    @Column(nullable = false)
    @Builder.Default
    private Long createdAt = System.currentTimeMillis();

    @Column
    private Long lastSeen;

    @Column
    @Builder.Default
    private String status = "Hey there!";

    @Column(nullable = false)
    @Builder.Default
    private Boolean isActive = true;
}
