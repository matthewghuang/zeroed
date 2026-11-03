package com.zeroed.app.models;

import java.io.Serializable;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable //GroupMembershipID uses user and group IDs as a composite primary key for the GroupMembership entity. 
            // It is marked as @Embeddable to indicate that it can be embedded in another entity.
public class GroupMembershipId implements Serializable {
    @Column(name = "user_id")
    private Long userId;

    @Column(name = "group_id")
    private Long groupId;

    protected GroupMembershipId() {}

    public GroupMembershipId(Long userId, Long groupId) {
        this.userId = userId;
        this.groupId = groupId;
    }

    @Override
    public boolean equals(Object o) {// if IDs match
        if (this == o) return true;
        if (!(o instanceof GroupMembershipId other)) return false;
        return Objects.equals(userId, other.userId) && Objects.equals(groupId, other.groupId);
    }

    @Override 
    public int hashCode() {
        return Objects.hash(userId, groupId);
    }
}