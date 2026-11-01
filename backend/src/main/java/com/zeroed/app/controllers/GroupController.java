package com.zeroed.app.controllers;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.zeroed.app.dto.CreateGroupRequest;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/groups")
public class GroupController {

    @PostMapping
    public ResponseEntity<Void> createGroup(@Valid @RequestBody CreateGroupRequest request) {
        // TODO (will update and adapt after merge):
        // 1. get the current user from the session
        // 2. call GroupService.createGroup(request.name(), currentUser)
        // 3. return 201 with the new group
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
    }
}