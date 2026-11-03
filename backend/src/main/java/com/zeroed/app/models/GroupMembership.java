package com.zeroed.app.models;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;

@Entity
@Table(name = "group_memberships")
// many-to-many relationship between User and Group
// stores each user's membership in a group, with some additional information. eg. when they joined the group
public class GroupMembership {

    @EmbeddedId
    private GroupMembershipId id;

    @NotNull
    @MapsId("userId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @NotNull
    @MapsId("groupId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "group_id", nullable = false)
    private Group group;

    @Column(name = "joined_at", nullable = false, updatable = false)
    private Instant joinedAt;

    protected GroupMembership() {}

    public GroupMembership(User user, Group group) {
        this.user = user;
        this.group = group;
        this.id = new GroupMembershipId(user.getId(), group.getId());
        this.joinedAt = Instant.now();
    }

	public GroupMembershipId getId() {
		return this.id;
	}

	public void setId(GroupMembershipId id) {
		this.id = id;
	}

	public User getUser() {
		return this.user;
	}

	public void setUser(User user) {
		this.user = user;
	}

	public Group getGroup() {
		return this.group;
	}

	public void setGroup(Group group) {
		this.group = group;
	}

	public Instant getJoinedAt() {
		return this.joinedAt;
	}

	public void setJoinedAt(Instant joinedAt) {
		this.joinedAt = joinedAt;
	}
}