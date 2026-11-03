package com.zeroed.app.models;

import java.io.Serializable;
import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;

@Embeddable
class GroupMembershipId implements Serializable {
    @Column(name = "user_id")
    private Long userId;

    @Column(name = "group_id")
    private Long groupId;

    protected GroupMembershipId() {}

    public GroupMembershipId(Long userId, Long groupId) {
        this.userId = userId;
        this.groupId = groupId;
    }
}

@Entity 
@Table(name = "group_memberships")
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
}
