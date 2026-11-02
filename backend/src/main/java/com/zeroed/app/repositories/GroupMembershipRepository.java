package com.zeroed.app.repositories;

import org.springframework.data.jpa.repository.JpaRepository;

import com.zeroed.app.models.GroupMembership;
import com.zeroed.app.models.GroupMembershipId;

public interface GroupMembershipRepository extends JpaRepository<GroupMembership, GroupMembershipId> {
}