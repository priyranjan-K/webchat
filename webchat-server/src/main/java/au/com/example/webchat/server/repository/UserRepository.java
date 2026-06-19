package au.com.example.webchat.server.repository;

import au.com.example.webchat.server.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByPhoneNumber(String phoneNumber);
    Boolean existsByPhoneNumber(String phoneNumber);
}
