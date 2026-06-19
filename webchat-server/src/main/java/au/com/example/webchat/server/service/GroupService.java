package au.com.example.webchat.server.service;

import au.com.example.webchat.server.websocket.ChatGroup;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory registry for active chat groups.
 * <p>
 * Groups are stored in a {@link ConcurrentHashMap} to allow safe concurrent access
 * by multiple WebSocket handler threads. Group metadata (name, creator, membership)
 * lives here; message history is persisted separately in the database.
 * <p>
 * <strong>Note:</strong> Because this is in-memory only, all groups are lost on
 * application restart. Persist groups to the database if durability is required.
 */
@Service
public class GroupService {

    /** Thread-safe map from groupId → ChatGroup. */
    private final Map<String, ChatGroup> groups = new ConcurrentHashMap<>();

    /**
     * Creates a new group and registers it in the in-memory store.
     *
     * @param groupId   unique group identifier (UUID)
     * @param groupName human-readable group name
     * @param creator   phone number of the creating user
     * @return the newly created {@link ChatGroup}
     */
    public ChatGroup createGroup(String groupId, String groupName, String creator) {
        ChatGroup group = new ChatGroup(groupId, groupName, creator);
        groups.put(groupId, group);
        return group;
    }

    /**
     * Looks up a group by its identifier.
     *
     * @param groupId the group ID to look up
     * @return an {@link Optional} containing the group, or empty if not found
     */
    public Optional<ChatGroup> findGroup(String groupId) {
        return Optional.ofNullable(groups.get(groupId));
    }

    /**
     * Returns a snapshot of all currently active groups.
     *
     * @return unmodifiable view of group values
     */
    public Collection<ChatGroup> getAllGroups() {
        return groups.values();
    }

    /**
     * Adds a member to an existing group. No-op if the group does not exist.
     *
     * @param groupId     the target group ID
     * @param phoneNumber the phone number to add
     */
    public void addMemberToGroup(String groupId, String phoneNumber) {
        findGroup(groupId).ifPresent(group -> group.addMember(phoneNumber));
    }

    /**
     * Removes a member from an existing group.
     * If removing the last member, the group is automatically deleted.
     *
     * @param groupId     the target group ID
     * @param phoneNumber the phone number to remove
     */
    public void removeMemberFromGroup(String groupId, String phoneNumber) {
        findGroup(groupId).ifPresent(group -> {
            group.removeMember(phoneNumber);
            if (group.getMembers().isEmpty()) {
                groups.remove(groupId);
            }
        });
    }

    /**
     * Returns {@code true} if a group with the given ID exists.
     *
     * @param groupId the group ID to check
     */
    public boolean groupExists(String groupId) {
        return groups.containsKey(groupId);
    }

    /**
     * Forcefully removes a group from the registry (e.g. when the creator deletes it).
     *
     * @param groupId the ID of the group to delete
     */
    public void deleteGroup(String groupId) {
        groups.remove(groupId);
    }

    /**
     * Returns a defensive copy of the member set for a given group.
     *
     * @param groupId the group ID
     * @return set of member phone numbers, or an empty set if the group doesn't exist
     */
    public Set<String> getGroupMembers(String groupId) {
        return findGroup(groupId)
                .map(g -> new HashSet<>(g.getMembers()))
                .orElse(new HashSet<>());
    }
}
