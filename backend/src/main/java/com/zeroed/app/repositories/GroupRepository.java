package com.zeroed.app.repositories;

import org.springframework.data.jpa.repository.JpaRepository;

import com.zeroed.app.models.Group;

public interface GroupRepository extends JpaRepository<Group, Long> {
}
