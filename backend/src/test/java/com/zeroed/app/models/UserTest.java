package com.zeroed.app.models;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.fail;


import java.time.Instant;

import org.junit.jupiter.api.Test;

public class UserTest {
    @Test
    void storesEmailAndId(){
        User usr = new User("boblennon@gmail.com", "abcd1234"); 
        
        assertEquals("boblennon@gmail.com", usr.getEmail());
        assertEquals("abcd1234", usr.getPasswordHash());
    }
}
