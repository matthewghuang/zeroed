package com.zeroed.app.models;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
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

	@NotEmpty
	@Column(name = "created_by", nullable = false, updatable = false)
	private Long createdBy;

	protected Group() {

	}

	public Group(String name, User creator) {
		this.name = name;
		this.createdBy = creator.getId();
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

	public Long getCreatedBy() {
		return this.createdBy;
	}
}
