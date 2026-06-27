package au.com.example.webchat.server.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "rsa_keys")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RsaKey {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Lob
    @Column(nullable = false, columnDefinition = "TEXT")
    private String publicKey;

    @Lob
    @Column(nullable = false, columnDefinition = "TEXT")
    private String privateKey;

    @Column(nullable = false)
    @Builder.Default
    private Long createdAt = System.currentTimeMillis();
}
