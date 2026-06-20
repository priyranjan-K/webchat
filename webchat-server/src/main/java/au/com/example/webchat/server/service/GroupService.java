package au.com.example.webchat.server.service;

import au.com.example.webchat.server.model.GroupEntity;
import au.com.example.webchat.server.model.GroupMemberEntity;
import au.com.example.webchat.server.repository.GroupMemberRepository;
import au.com.example.webchat.server.repository.GroupRepository;
import au.com.example.webchat.server.websocket.ChatGroup;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GroupService {

    private final GroupRepository groupRepository;
    private final GroupMemberRepository groupMemberRepository;

    @Transactional
    public ChatGroup createGroup(String groupId, String groupName, String creator) {
        GroupEntity groupEntity = GroupEntity.builder()
                .groupId(groupId)
                .groupName(groupName)
                .creator(creator)
                .createdAt(System.currentTimeMillis())
                .build();
        groupRepository.save(groupEntity);

        GroupMemberEntity memberEntity = GroupMemberEntity.builder()
                .groupId(groupId)
                .phoneNumber(creator)
                .isAdmin(true)
                .isCreator(true)
                .hasLeft(false)
                .build();
        groupMemberRepository.save(memberEntity);

        return findGroup(groupId).orElse(null);
    }

    public Optional<ChatGroup> findGroup(String groupId) {
        return groupRepository.findByGroupId(groupId).map(groupEntity -> {
            List<GroupMemberEntity> members = groupMemberRepository.findByGroupId(groupId);
            Set<String> activeMembers = members.stream()
                    .filter(m -> !m.getHasLeft())
                    .map(GroupMemberEntity::getPhoneNumber)
                    .collect(Collectors.toCollection(CopyOnWriteArraySet::new));
            Set<String> admins = members.stream()
                    .filter(m -> !m.getHasLeft() && m.getIsAdmin())
                    .map(GroupMemberEntity::getPhoneNumber)
                    .collect(Collectors.toCollection(CopyOnWriteArraySet::new));
            Set<String> leftMembers = members.stream()
                    .filter(GroupMemberEntity::getHasLeft)
                    .map(GroupMemberEntity::getPhoneNumber)
                    .collect(Collectors.toCollection(CopyOnWriteArraySet::new));

            return ChatGroup.builder()
                    .groupId(groupEntity.getGroupId())
                    .groupName(groupEntity.getGroupName())
                    .creator(groupEntity.getCreator())
                    .createdAt(groupEntity.getCreatedAt())
                    .members(activeMembers)
                    .admins(admins)
                    .leftMembers(leftMembers)
                    .build();
        });
    }

    public Collection<ChatGroup> getAllGroups() {
        return groupRepository.findAll().stream()
                .map(g -> findGroup(g.getGroupId()).orElse(null))
                .filter(Objects::nonNull)
                .toList();
    }

    @Transactional
    public void addMemberToGroup(String groupId, String phoneNumber) {
        groupMemberRepository.findByGroupIdAndPhoneNumber(groupId, phoneNumber)
                .ifPresentOrElse(
                        member -> {
                            member.setHasLeft(false);
                            groupMemberRepository.save(member);
                        },
                        () -> {
                            GroupMemberEntity member = GroupMemberEntity.builder()
                                    .groupId(groupId)
                                    .phoneNumber(phoneNumber)
                                    .isAdmin(false)
                                    .isCreator(false)
                                    .hasLeft(false)
                                    .build();
                            groupMemberRepository.save(member);
                        }
                );
    }

    @Transactional
    public void removeMemberFromGroup(String groupId, String phoneNumber) {
        groupMemberRepository.findByGroupIdAndPhoneNumber(groupId, phoneNumber)
                .ifPresent(member -> {
                    member.setHasLeft(true);
                    member.setIsAdmin(false);
                    groupMemberRepository.save(member);

                    // Ownership transfer if creator leaves
                    GroupEntity group = groupRepository.findByGroupId(groupId).orElse(null);
                    if (group != null && phoneNumber.equals(group.getCreator())) {
                        List<GroupMemberEntity> allMembers = groupMemberRepository.findByGroupId(groupId);
                        List<GroupMemberEntity> activeMembers = allMembers.stream()
                                .filter(m -> !m.getHasLeft())
                                .toList();

                        if (!activeMembers.isEmpty()) {
                            GroupMemberEntity newCreator = activeMembers.stream()
                                    .filter(GroupMemberEntity::getIsAdmin)
                                    .findFirst()
                                    .orElse(activeMembers.get(0));

                            newCreator.setIsAdmin(true);
                            newCreator.setIsCreator(true);
                            groupMemberRepository.save(newCreator);

                            group.setCreator(newCreator.getPhoneNumber());
                            groupRepository.save(group);
                        } else {
                            // No active members left, delete group
                            deleteGroup(groupId);
                        }
                    }
                });
    }

    public boolean groupExists(String groupId) {
        return groupRepository.findByGroupId(groupId).isPresent();
    }

    @Transactional
    public void deleteGroup(String groupId) {
        groupRepository.deleteByGroupId(groupId);
        groupMemberRepository.deleteByGroupId(groupId);
    }

    public Set<String> getGroupMembers(String groupId) {
        return groupMemberRepository.findByGroupId(groupId).stream()
                .filter(m -> !m.getHasLeft())
                .map(GroupMemberEntity::getPhoneNumber)
                .collect(Collectors.toSet());
    }

    @Transactional
    public void promoteToAdmin(String groupId, String phoneNumber) {
        groupMemberRepository.findByGroupIdAndPhoneNumber(groupId, phoneNumber)
                .ifPresent(member -> {
                    if (!member.getHasLeft()) {
                        member.setIsAdmin(true);
                        groupMemberRepository.save(member);
                    }
                });
    }

    @Transactional
    public void demoteToNormalUser(String groupId, String phoneNumber) {
        groupMemberRepository.findByGroupIdAndPhoneNumber(groupId, phoneNumber)
                .ifPresent(member -> {
                    GroupEntity group = groupRepository.findByGroupId(groupId).orElse(null);
                    if (group != null && !phoneNumber.equals(group.getCreator())) {
                        member.setIsAdmin(false);
                        groupMemberRepository.save(member);
                    }
                });
    }
}
