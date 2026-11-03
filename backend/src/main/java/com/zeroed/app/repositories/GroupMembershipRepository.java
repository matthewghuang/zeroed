package com.zeroed.app.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.zeroed.app.models.Group;
import com.zeroed.app.models.GroupMembership;
import com.zeroed.app.models.GroupMembershipId;
import com.zeroed.app.models.User;

public interface GroupMembershipRepository extends JpaRepository<GroupMembership, GroupMembershipId> {

    @Query("select membership.group from GroupMembership membership where membership.user.id = :userId")
    List<Group> findGroupsByUserId(@Param("userId") Long userId);

    @Query("select membership.user from GroupMembership membership where membership.group.id = :groupId")
    List<User> findMembersByGroupId(@Param("groupId") Long groupId);
}
