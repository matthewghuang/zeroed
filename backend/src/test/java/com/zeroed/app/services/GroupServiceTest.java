package com.zeroed.app.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.zeroed.app.models.Group;
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
