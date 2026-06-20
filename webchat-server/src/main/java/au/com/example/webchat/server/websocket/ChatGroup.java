package au.com.example.webchat.server.websocket;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * In-memory representation of a chat group.
 * <p>
 * <strong>Not a JPA entity</strong> — group membership is tracked in memory by
 * {@link au.com.example.webchat.server.service.GroupService}.
 * Messages are persisted separately as {@link au.com.example.webchat.server.model.MessageEntity}
 * records so history survives restarts.
 * <p>
 * Thread-safety: {@link CopyOnWriteArraySet} is used for the member set so that
 * concurrent WebSocket handler invocations can safely iterate and mutate it.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatGroup {

    private String groupId;
    private String groupName;
    private String creator;
    private long   createdAt;
    private Set<String> members;
    private Set<String> admins;
    @Builder.Default
    private Set<String> leftMembers = new CopyOnWriteArraySet<>();

    /**
     * Convenience constructor — initialises timestamps, creates the member and admin sets,
     * and adds the creator as the first member and admin.
     *
     * @param groupId   unique group identifier (UUID)
     * @param groupName human-readable group name
     * @param creator   phone number of the user who created the group
     */
    public ChatGroup(String groupId, String groupName, String creator) {
        this.groupId   = groupId;
        this.groupName = groupName;
        this.creator   = creator;
        this.createdAt = System.currentTimeMillis();
        this.members   = new CopyOnWriteArraySet<>();
        this.members.add(creator);
        this.admins    = new CopyOnWriteArraySet<>();
        this.admins.add(creator);
        this.leftMembers = new CopyOnWriteArraySet<>();
    }

    /** Adds a member to this group. No-op if already a member. */
    public void addMember(String phoneNumber) {
        this.members.add(phoneNumber);
        if (this.leftMembers != null) {
            this.leftMembers.remove(phoneNumber);
        }
    }

    /** Removes a member from this group and automatically removes admin status. Transfess ownership if creator leaves. */
    public void removeMember(String phoneNumber) {
        this.members.remove(phoneNumber);
        this.admins.remove(phoneNumber);
        if (this.leftMembers == null) {
            this.leftMembers = new CopyOnWriteArraySet<>();
        }
        this.leftMembers.add(phoneNumber);
        if (phoneNumber.equals(creator) && !this.members.isEmpty()) {
            String newCreator = this.admins.stream().findFirst().orElse(null);
            if (newCreator == null) {
                newCreator = this.members.iterator().next();
            }
            this.creator = newCreator;
            this.admins.add(newCreator);
        }
    }

    /** Returns {@code true} if the given phone number is a member of this group. */
    public boolean hasMember(String phoneNumber) {
        return this.members.contains(phoneNumber);
    }

    /** Promotes a member to admin. */
    public void promoteToAdmin(String phoneNumber) {
        if (this.members.contains(phoneNumber)) {
            this.admins.add(phoneNumber);
        }
    }

    /** Demotes an admin back to normal user. Creator cannot be demoted. */
    public void demoteToNormalUser(String phoneNumber) {
        if (!phoneNumber.equals(creator)) {
            this.admins.remove(phoneNumber);
        }
    }

    /** Checks if a member is an admin. */
    public boolean isAdmin(String phoneNumber) {
        return this.admins.contains(phoneNumber);
    }
}
