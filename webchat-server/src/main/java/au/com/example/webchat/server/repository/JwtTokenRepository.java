package au.com.example.webchat.server.repository;

import au.com.example.webchat.server.model.JwtToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface JwtTokenRepository extends JpaRepository<JwtToken, Long> {
    boolean existsByToken(String token);

    @Transactional
    void deleteByToken(String token);

    @Transactional
    void deleteByPhoneNumber(String phoneNumber);

    @Transactional
    void deleteByExpiryLessThan(Long expiryLimit);
}
