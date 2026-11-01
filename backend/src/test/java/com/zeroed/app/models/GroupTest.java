package com.zeroed.app.models;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.fail;


import java.time.Instant;

import org.junit.jupiter.api.Test;

public class GroupTest {
    @Test
    void createGroup(){
        User guy = new User("epicguy@gmail.com", "something");
        Group test = new Group("AWESOME Group", guy);
        Instant creationDate = Instant.now();

        test.onCreate();

        assertEquals("AWESOME Group", test.getName());
        assertEquals(guy, test.getCreatedBy());
        assertEquals(creationDate, test.getCreatedAt());


    }

    @Test
    void updateGroup(){
        User guy = new User("epicguy@gmail.com", "something");
        Group test = new Group("AWESOME Group", guy);
        Instant creationDate = Instant.now();

        test.onCreate();

        test.setName("the New GROUP!");

        assertEquals("the New GROUP!", test.getName());
        assertEquals(guy, test.getCreatedBy());
        assertEquals(creationDate, test.getCreatedAt());


    }

}
