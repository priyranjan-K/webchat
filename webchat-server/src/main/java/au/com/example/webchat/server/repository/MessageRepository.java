package au.com.example.webchat.server.repository;

import au.com.example.webchat.server.model.MessageEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Persistence operations for {@link MessageEntity} records.
 */
@Repository
public interface MessageRepository extends JpaRepository<MessageEntity, Long> {

    /**
     * Returns the DM history between two users from the perspective of user1, ordered by timestamp ascending.
     */
    @Query("SELECT m FROM MessageEntity m " +
           "WHERE (m.sender = :user1 AND m.recipientId = :user2 AND m.deletedBySender = false) " +
           "   OR (m.sender = :user2 AND m.recipientId = :user1 AND m.deletedByRecipient = false) " +
           "ORDER BY m.timestamp ASC")
    List<MessageEntity> findDmHistory(@Param("user1") String user1, @Param("user2") String user2);

    /**
     * Returns all DM messages between two users regardless of deletion flags (used for updating deletion state).
     */
    @Query("SELECT m FROM MessageEntity m " +
           "WHERE (m.sender = :user1 AND m.recipientId = :user2) " +
           "   OR (m.sender = :user2 AND m.recipientId = :user1)")
    List<MessageEntity> findDmHistoryAll(@Param("user1") String user1, @Param("user2") String user2);

    List<MessageEntity> findByGroupIdOrderByTimestampAsc(String groupId);

    /**
     * Returns every distinct phone number that the given user has exchanged active DMs with.
     */
    @Query("SELECT DISTINCT CASE WHEN m.sender = :user THEN m.recipientId ELSE m.sender END " +
           "FROM MessageEntity m " +
           "WHERE m.recipientId IS NOT NULL " +
           "  AND ((m.sender = :user AND m.deletedBySender = false) " +
           "    OR (m.recipientId = :user AND m.deletedByRecipient = false))")
    List<String> findDistinctChatPartners(@Param("user") String user);

    @Transactional
    @Modifying
    @Query("DELETE FROM MessageEntity m WHERE m.groupId = :groupId")
    void deleteByGroupId(@Param("groupId") String groupId);
}
