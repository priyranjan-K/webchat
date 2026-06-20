package au.com.example.webchat.server.repository;

import au.com.example.webchat.server.model.GroupMemberEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GroupMemberRepository extends JpaRepository<GroupMemberEntity, Long> {
    List<GroupMemberEntity> findByGroupId(String groupId);
    Optional<GroupMemberEntity> findByGroupIdAndPhoneNumber(String groupId, String phoneNumber);
    void deleteByGroupId(String groupId);
    void deleteByGroupIdAndPhoneNumber(String groupId, String phoneNumber);
}
