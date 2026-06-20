package au.com.example.webchat.server.repository;

import au.com.example.webchat.server.model.GroupEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface GroupRepository extends JpaRepository<GroupEntity, Long> {
    Optional<GroupEntity> findByGroupId(String groupId);
    void deleteByGroupId(String groupId);
}
