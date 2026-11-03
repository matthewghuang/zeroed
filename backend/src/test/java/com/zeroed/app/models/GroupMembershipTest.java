package com.zeroed.app.models;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.fail;


import java.time.Instant;

import org.junit.jupiter.api.Test;

public class GroupMembershipTest {
    @Test
    void createGroup(){
        User usr = new User("johndoe@gmail.com", "a1b2c3d4");
        Group grp = new Group("test group", usr);
        GroupMembershipId id = new GroupMembershipId(usr.getId(), grp.getId());
        Instant join = Instant.now();

        GroupMembership test = new GroupMembership(usr, grp);
        
        assertEquals(usr, test.getUser());
        assertEquals(grp, test.getGroup());
        assertEquals(id, test.getId());
        assertEquals(join, test.getJoinedAt());

    }
    
}
