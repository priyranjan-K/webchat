package au.com.example.webchat.server.repository;

import au.com.example.webchat.server.model.RsaKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RsaKeyRepository extends JpaRepository<RsaKey, Long> {
    Optional<RsaKey> findFirstByOrderByCreatedAtDesc();
}
