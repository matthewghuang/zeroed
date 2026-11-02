package com.zeroed.app.services;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.zeroed.app.models.Group;
import com.zeroed.app.models.GroupMembership;
import com.zeroed.app.models.User;
import com.zeroed.app.repositories.GroupMembershipRepository;
import com.zeroed.app.repositories.GroupRepository;

@Service
public class GroupService {
    private final GroupRepository groupRepository;
    private final GroupMembershipRepository groupMembershipRepository;

    public GroupService(GroupRepository groupRepository,
                        GroupMembershipRepository groupMembershipRepository) {
        this.groupRepository = groupRepository;
        this.groupMembershipRepository = groupMembershipRepository;
    }

    @Transactional
    public Group createGroup(String name, User creator) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("A group name is required, please enter a group name.");
        }

        // save a new group
        Group group = groupRepository.save(new Group(name.trim(), creator));

        // add the creator to the group's membership, (the group now has its generated id, which GroupMembership needs)
        groupMembershipRepository.save(new GroupMembership(creator, group));

        return group;
    }
}