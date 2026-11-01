package com.zeroed.app.controllers;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class SpaControllerTest {
    private MockMvc fakeMvc;

    @BeforeEach
    void setUp() {
        fakeMvc = MockMvcBuilders.standaloneSetup(new SpaController()).build();
    }

    @Test
    void forwardsToIndex() throws Exception {
        fakeMvc.perform(get("/settings")).andExpect(status().isOk()).andExpect(forwardedUrl("/index.html"));

        fakeMvc.perform(get("/users/42/profile")).andExpect(status().isOk()).andExpect(forwardedUrl("/index.html"));
    }

    @Test
    void noForwardReservedRoutes() throws Exception {
        fakeMvc.perform(get("/api/users")).andExpect(status().isNotFound());

        fakeMvc.perform(get("/assets/app.js")).andExpect(status().isNotFound());
    }
}
