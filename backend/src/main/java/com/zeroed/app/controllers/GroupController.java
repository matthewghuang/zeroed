package com.zeroed.app.controllers;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.zeroed.app.dto.CreateGroupRequest;
import com.zeroed.app.models.Group;
import com.zeroed.app.models.User;
import com.zeroed.app.repositories.UserRepository;
import com.zeroed.app.services.GroupService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/groups")
public class GroupController {
    private final GroupService groupService;
    private final UserRepository userRepository;

    public GroupController(GroupService groupService, UserRepository userRepository) {
        this.groupService = groupService;
        this.userRepository = userRepository;
    }

    @PostMapping
    public ResponseEntity<?> createGroup(@Valid @RequestBody CreateGroupRequest request,
                                         Authentication authentication) {
        // get the current user from the session
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        // principal name is user email (see CustomUserDetailsService)
        User currentUser = userRepository.findByEmail(authentication.getName()).orElse(null);
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        //call GroupService.createGroup(request.name(), currentUser)
        Group group = groupService.createGroup(request.name(), currentUser);

        // return 201 with the new group
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("id", group.getId(), "name", group.getName()));
    }
}