package com.zeroed.app.services;

import org.springframework.stereotype.Service;

import com.zeroed.app.repositories.GroupRepository;

@Service
public class GroupService {
    private final GroupRepository groupRepository;

    public GroupService(GroupRepository groupRepository) {
        this.groupRepository = groupRepository;
    }

    // TODO: createGroup(name, creator)
    // 1. save a new Group
    // 2. add the creator to the group's membership (via some sort of group membership repository)

}