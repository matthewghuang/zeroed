package com.zeroed.app.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import com.zeroed.app.dto.CreateGroupRequest;
import com.zeroed.app.dto.GroupResponse;
import com.zeroed.app.models.Group;
import com.zeroed.app.models.User;
import com.zeroed.app.repositories.UserRepository;
import com.zeroed.app.services.GroupService;

@ExtendWith(MockitoExtension.class)
class GroupControllerTest {
    private static final String EMAIL = "alex@example.com";
    private static final Instant CREATED_AT = Instant.parse("2026-10-28T18:30:00Z");

    @Mock
    private GroupService groupService;

    @Mock
    private UserRepository userRepository;

    private GroupController controller;

    @BeforeEach
    void setUp() {
        controller = new GroupController(groupService, userRepository);
    }

    @Test
    void createGroupReturnsCreatedGroupResponse() {
        User user = signedInUser();
        Group createdGroup = group(7L, "Vancouver Trip");
        when(groupService.createGroup("Vancouver Trip", user)).thenReturn(createdGroup);

        var response = controller.createGroup(new CreateGroupRequest("Vancouver Trip"), authentication());

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals(new GroupResponse(7L, "Vancouver Trip", CREATED_AT), response.getBody());
        verify(groupService).createGroup("Vancouver Trip", user);
    }

    @Test
    void getGroupsReturnsGroupResponsesForCurrentUser() {
        User user = signedInUser();
        List<Group> groups = List.of(group(7L, "Vancouver Trip"), group(8L, "Housemates"));
        when(groupService.getGroupsForUser(user)).thenReturn(groups);

        var response = controller.getGroups(authentication());

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(List.of(
                new GroupResponse(7L, "Vancouver Trip", CREATED_AT),
                new GroupResponse(8L, "Housemates", CREATED_AT)), response.getBody());
        verify(groupService).getGroupsForUser(user);
    }

    @Test
    void getGroupsAcceptsAdminAuthentication() {
        User admin = signedInUser();
        List<Group> groups = List.of(group(7L, "Vancouver Trip"), group(8L, "Housemates"));
        when(groupService.getGroupsForUser(admin)).thenReturn(groups);
        Authentication adminAuthentication = UsernamePasswordAuthenticationToken.authenticated(
                EMAIL, null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));

        var response = controller.getGroups(adminAuthentication);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(List.of(
                new GroupResponse(7L, "Vancouver Trip", CREATED_AT),
                new GroupResponse(8L, "Housemates", CREATED_AT)), response.getBody());
        verify(groupService).getGroupsForUser(admin);
    }

    @Test
    void getGroupsReturnsEmptyListWhenUserHasNoGroups() {
        User user = signedInUser();
        when(groupService.getGroupsForUser(user)).thenReturn(List.of());

        var response = controller.getGroups(authentication());

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(List.of(), response.getBody());
    }

    @ParameterizedTest
    @MethodSource("unauthorizedAuthentications")
    void createGroupRejectsUnauthorizedAuthentication(Authentication authentication) {
        var response = controller.createGroup(new CreateGroupRequest("Vancouver Trip"), authentication);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertNull(response.getBody());
        verifyNoInteractions(userRepository, groupService);
    }

    @ParameterizedTest
    @MethodSource("unauthorizedAuthentications")
    void getGroupsRejectsUnauthorizedAuthentication(Authentication authentication) {
        var response = controller.getGroups(authentication);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertNull(response.getBody());
        verifyNoInteractions(userRepository, groupService);
    }

    @Test
    void createGroupRejectsSessionWhoseUserNoLongerExists() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

        var response = controller.createGroup(new CreateGroupRequest("Vancouver Trip"), authentication());

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertNull(response.getBody());
        verifyNoInteractions(groupService);
    }

    @Test
    void getGroupsRejectsSessionWhoseUserNoLongerExists() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

        var response = controller.getGroups(authentication());

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertNull(response.getBody());
        verifyNoInteractions(groupService);
    }

    private User signedInUser() {
        User user = new User(EMAIL, "encoded-password");
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        return user;
    }

    private static Group group(Long id, String name) {
        Group group = mock(Group.class);
        when(group.getId()).thenReturn(id);
        when(group.getName()).thenReturn(name);
        when(group.getCreatedAt()).thenReturn(CREATED_AT);
        return group;
    }

    private static Authentication authentication() {
        return UsernamePasswordAuthenticationToken.authenticated(
                EMAIL, null, List.of(new SimpleGrantedAuthority("ROLE_REGULAR")));
    }

    private static Stream<Authentication> unauthorizedAuthentications() {
        return Stream.of(
                null,
                UsernamePasswordAuthenticationToken.unauthenticated(EMAIL, null),
                new AnonymousAuthenticationToken("test-key", "anonymousUser",
                        List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS"))));
    }
}
