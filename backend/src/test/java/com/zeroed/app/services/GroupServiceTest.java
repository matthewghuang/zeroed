package com.zeroed.app.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.zeroed.app.models.Group;
import com.zeroed.app.models.GroupMembershipId;
import com.zeroed.app.models.User;
import com.zeroed.app.models.UserRole;
import com.zeroed.app.repositories.GroupMembershipRepository;
import com.zeroed.app.repositories.GroupRepository;

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

    @Test
    void getGroupMembersReturnsMembersWhenCallerBelongsToGroup() {
        when(currentUser.getId()).thenReturn(42L);
        when(groupMembershipRepository.existsById(new GroupMembershipId(42L, 7L))).thenReturn(true);
        List<User> members = List.of(currentUser, new User("sam@example.com", "encoded-password"));
        when(groupMembershipRepository.findMembersByGroupId(7L)).thenReturn(members);
        GroupService service = new GroupService(groupRepository, groupMembershipRepository);

        assertEquals(members, service.getGroupMembers(7L, currentUser));
        verify(groupMembershipRepository).findMembersByGroupId(7L);
    }

    @ParameterizedTest
    @ValueSource(longs = {7L, 999L})
    void getGroupMembersHidesGroupsWithoutCallerMembership(Long groupId) {
        when(currentUser.getId()).thenReturn(42L);
        when(groupMembershipRepository.existsById(new GroupMembershipId(42L, groupId))).thenReturn(false);
        GroupService service = new GroupService(groupRepository, groupMembershipRepository);

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
        GroupService service = new GroupService(groupRepository, groupMembershipRepository);

        List<Group> result = service.getGroupsForUser(admin);

        assertEquals(allGroups, result);
        verify(groupRepository).findAll();
        verifyNoInteractions(groupMembershipRepository);
    }
}
