package com.zeroed.app.models;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.fail;


import java.time.Instant;

import org.junit.jupiter.api.Test;

public class UserTest {
    @Test
    void storesValuesProperly(){
        User usr = new User("boblennon@gmail.com", "abcd1234"); 
        
        assertEquals("boblennon@gmail.com", usr.getEmail());
        assertEquals("abcd1234", usr.getPasswordHash());
        assertEquals(UserRole.REGULAR, usr.getRole());
        assertNull(usr.getId());
        assertNull(usr.getCreatedAt());
    }

    @Test
    void updateValues(){
        User usr = new User("boblennon@gmail.com", "abcd1234"); 
        
        usr.setEmail("newmail@gmail.com");
        usr.setPasswordHash("newpass");

        assertEquals("newmail@gmail.com", usr.getEmail());
        assertEquals("newpass", usr.getPasswordHash());
        assertEquals(UserRole.REGULAR, usr.getRole());
        assertNull(usr.getId());
        assertNull(usr.getCreatedAt());
    }


    @Test
    void createCreationTime() {
        User usr = new User("something@ow.com", "123bcd");
        Instant before = Instant.now();

        usr.onCreate();

        assertNotNull(usr.getCreatedAt());
        assertFalse(usr.getCreatedAt().isBefore(before));
    }

    @Test
    void invalidEmail(){
        String og = "Hello";
        String test = String.format("%-257s", og); 

        try{
            User usr = new User(test, "abc123");
            usr.getEmail();
            fail();
        } catch (IllegalArgumentException e){
            assertEquals("email length exceeded", e.getMessage());
        }
    }

    @Test
    void invalidPasswordHash(){
        String og = "Hello";
        String test = String.format("%-257s", og); 

        try{
            User usr = new User("idk@gmail.com", test);
            usr.getPasswordHash();
            fail();
        } catch (IllegalArgumentException e){
            assertEquals("passwordhash length exceeded", e.getMessage());
        }
    }


    
}
