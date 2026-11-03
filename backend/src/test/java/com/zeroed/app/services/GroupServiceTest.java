package com.zeroed.app.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.test.util.ReflectionTestUtils;

import com.zeroed.app.models.Group;
import com.zeroed.app.models.GroupMembershipId;
import com.zeroed.app.models.GroupMembership;
import com.zeroed.app.models.User;
import com.zeroed.app.models.UserRole;
import com.zeroed.app.repositories.GroupMembershipRepository;
import com.zeroed.app.repositories.GroupRepository;
import com.zeroed.app.repositories.UserRepository;

@ExtendWith(MockitoExtension.class)
class GroupServiceTest {
    @Mock
    private GroupRepository groupRepository;

    @Mock
    private GroupMembershipRepository groupMembershipRepository;

    @Mock
    private User admin;

    @Mock
    private User currentUser;

    @Mock
    private UserRepository userRepository;

    @Mock
    private User targetUser;

    @Test
    void addGroupMemberNormalizesEmailAndSavesMembership() {
        Group group = groupForMemberAddition();
        when(userRepository.findByEmail("sam@example.com")).thenReturn(Optional.of(targetUser));
        when(targetUser.getId()).thenReturn(57L);
        GroupService service = new GroupService(groupRepository, groupMembershipRepository, userRepository);

        assertEquals(targetUser, service.addGroupMember(7L, " SAM@example.com ", currentUser));

        ArgumentCaptor<GroupMembership> captor = ArgumentCaptor.forClass(GroupMembership.class);
        verify(groupMembershipRepository).save(captor.capture());
        assertEquals(new GroupMembershipId(57L, group.getId()),
                ReflectionTestUtils.getField(captor.getValue(), "id"));
    }

    @Test
    void addGroupMemberRejectsDuplicateMembership() {
        groupForMemberAddition();
        when(userRepository.findByEmail("sam@example.com")).thenReturn(Optional.of(targetUser));
        when(targetUser.getId()).thenReturn(57L);
        when(groupMembershipRepository.existsById(new GroupMembershipId(57L, 7L))).thenReturn(true);
        GroupService service = new GroupService(groupRepository, groupMembershipRepository, userRepository);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service.addGroupMember(7L, "sam@example.com", currentUser));

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        verify(groupMembershipRepository, never()).save(any());
    }

    @Test
    void addGroupMemberRejectsUnknownEmail() {
        groupForMemberAddition();
        when(userRepository.findByEmail("missing@example.com")).thenReturn(Optional.empty());
        GroupService service = new GroupService(groupRepository, groupMembershipRepository, userRepository);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service.addGroupMember(7L, "missing@example.com", currentUser));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        verify(groupMembershipRepository, never()).save(any());
    }

    @Test
    void addGroupMemberRejectsCallerOutsideGroupBeforeLookingUpTarget() {
        when(currentUser.getId()).thenReturn(42L);
        GroupService service = new GroupService(groupRepository, groupMembershipRepository, userRepository);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service.addGroupMember(7L, "sam@example.com", currentUser));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        verifyNoInteractions(userRepository, groupRepository);
        verify(groupMembershipRepository, never()).save(any());
    }

    private Group groupForMemberAddition() {
        when(currentUser.getId()).thenReturn(42L);
        when(groupMembershipRepository.existsById(new GroupMembershipId(42L, 7L))).thenReturn(true);
        Group group = new Group("Trip", currentUser);
        ReflectionTestUtils.setField(group, "id", 7L);
        when(groupRepository.findById(7L)).thenReturn(Optional.of(group));
        return group;
    }

    @Test
    void getGroupMembersReturnsMembersWhenCallerBelongsToGroup() {
        when(currentUser.getId()).thenReturn(42L);
        when(groupMembershipRepository.existsById(new GroupMembershipId(42L, 7L))).thenReturn(true);
        List<User> members = List.of(currentUser, new User("sam@example.com", "encoded-password"));
        when(groupMembershipRepository.findMembersByGroupId(7L)).thenReturn(members);
        GroupService service = new GroupService(groupRepository, groupMembershipRepository, userRepository);

        assertEquals(members, service.getGroupMembers(7L, currentUser));
        verify(groupMembershipRepository).findMembersByGroupId(7L);
    }

    @ParameterizedTest
    @ValueSource(longs = {7L, 999L})
    void getGroupMembersHidesGroupsWithoutCallerMembership(Long groupId) {
        when(currentUser.getId()).thenReturn(42L);
        when(groupMembershipRepository.existsById(new GroupMembershipId(42L, groupId))).thenReturn(false);
        GroupService service = new GroupService(groupRepository, groupMembershipRepository, userRepository);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service.getGroupMembers(groupId, currentUser));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        verify(groupMembershipRepository, never()).findMembersByGroupId(anyLong());
    }

    @Test
    void getGroupsForAdminReturnsAllGroupsWithoutCheckingMembership() {
        when(admin.getRole()).thenReturn(UserRole.ADMIN);
        User otherUser = new User("other@example.com", "encoded-password");
        List<Group> allGroups = List.of(
                new Group("Vancouver Trip", otherUser),
                new Group("Housemates", otherUser));
        when(groupRepository.findAll()).thenReturn(allGroups);
        GroupService service = new GroupService(groupRepository, groupMembershipRepository, userRepository);

        List<Group> result = service.getGroupsForUser(admin);

        assertEquals(allGroups, result);
        verify(groupRepository).findAll();
        verifyNoInteractions(groupMembershipRepository);
    }
}
