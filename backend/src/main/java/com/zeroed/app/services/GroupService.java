package com.zeroed.app.services;

import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.transaction.annotation.Transactional;

import com.zeroed.app.models.Group;
import com.zeroed.app.models.GroupMembership;
import com.zeroed.app.models.GroupMembershipId;
import com.zeroed.app.models.User;
import com.zeroed.app.models.UserRole;
import com.zeroed.app.repositories.GroupMembershipRepository;
import com.zeroed.app.repositories.GroupRepository;
import com.zeroed.app.repositories.UserRepository;

@Service
public class GroupService {
    private final GroupRepository groupRepository;
    private final GroupMembershipRepository groupMembershipRepository;
    private final UserRepository userRepository;

    public GroupService(GroupRepository groupRepository,
                        GroupMembershipRepository groupMembershipRepository,
                        UserRepository userRepository) {
        this.groupRepository = groupRepository;
        this.groupMembershipRepository = groupMembershipRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public Group createGroup(String name, User creator) {
        if (name == null || name.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A group name is required, please enter a group name.");
        }

        // save a new group
        Group group = groupRepository.save(new Group(name.trim(), creator));

        // add the creator to the group's membership, (the group now has its generated id, which GroupMembership needs)
        groupMembershipRepository.save(new GroupMembership(creator, group));

        return group;
    }

    @Transactional(readOnly = true)
    public List<Group> getGroupsForUser(User user) {
        if (user.getRole() == UserRole.ADMIN) {
            return groupRepository.findAll();
        }
        return groupMembershipRepository.findGroupsByUserId(user.getId());
    }

    @Transactional(readOnly = true)
    public List<User> getGroupMembers(Long groupId, User user) {
        if (!groupMembershipRepository.existsById(new GroupMembershipId(user.getId(), groupId))) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Group not found");
        }
        return groupMembershipRepository.findMembersByGroupId(groupId);
    }

    @Transactional
    public User addGroupMember(Long groupId, String email, User currentUser) {
        if (!groupMembershipRepository.existsById(new GroupMembershipId(currentUser.getId(), groupId))) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Group not found");
        }

        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Group not found"));
        User member = userRepository.findByEmail(email.trim().toLowerCase(Locale.ROOT))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        if (groupMembershipRepository.existsById(new GroupMembershipId(member.getId(), groupId))) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "User is already a group member");
        }

        groupMembershipRepository.save(new GroupMembership(member, group));
        return member;
    }
}
