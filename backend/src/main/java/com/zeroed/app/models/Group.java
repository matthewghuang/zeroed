package com.zeroed.app.models;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

@Entity 
@Table(
	name = "groups"
)
public class Group {
	@Id 
	@GeneratedValue(strategy = GenerationType.IDENTITY)	
	private Long id;

	@NotBlank 
	@Size(max = 1024)
	@Column(name = "name", nullable = false, length = 1024)
	private String name;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)	
	@JoinColumn(name = "created_by_user_id", nullable = false)
	private User createdBy;

	protected Group() {

	}

	public Group(String name, User creator) {
		this.name = name;
		this.createdBy = creator;
	}

	@PrePersist
	protected void onCreate() {
		if (createdAt == null) {
			createdAt = Instant.now();
		}
	}

	public Long getId() {
		return this.id;
	}

	public String getName() {
		return this.name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Instant getCreatedAt() {
		return this.createdAt;
	}

	public User getCreatedBy() {
		return this.createdBy;
	}
}
