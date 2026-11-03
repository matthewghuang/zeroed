package com.zeroed.app.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.zeroed.app.models.User;
import com.zeroed.app.models.UserRole;
import com.zeroed.app.repositories.UserRepository;
import com.zeroed.app.services.AuthService;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {
    private static final String EMAIL = "alex@example.com";

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthenticationManager authenticationManager;

    private AuthController controller;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        controller = new AuthController(new AuthService(userRepository, passwordEncoder), authenticationManager);
        mvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @ParameterizedTest
    @EnumSource(UserRole.class)
    void meReturnsEmailAndCurrentDatabaseRole(UserRole role) throws Exception {
        User user = new User(EMAIL, "encoded-password");
        ReflectionTestUtils.setField(user, "role", role);
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));

        // The session role may be stale; the response should use the database role.
        mvc.perform(get("/api/auth/me").principal(authentication()))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.email").value(EMAIL))
                .andExpect(jsonPath("$.role").value(role.name()))
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    @ParameterizedTest
    @MethodSource("unauthorizedAuthentications")
    void meRejectsUnauthorizedAuthentication(Authentication authentication) {
        var response = controller.me(authentication);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertNull(response.getBody());
        verifyNoInteractions(userRepository);
    }

    @Test
    void meRejectsSessionWhoseUserNoLongerExists() throws Exception {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

        mvc.perform(get("/api/auth/me").principal(authentication()))
                .andExpect(status().isUnauthorized());
    }

    private static Authentication authentication() {
        return UsernamePasswordAuthenticationToken.authenticated(
                EMAIL, null, List.of(new SimpleGrantedAuthority("ROLE_REGULAR")));
    }

    private static Stream<Authentication> unauthorizedAuthentications() {
        return Stream.of(null,
                UsernamePasswordAuthenticationToken.unauthenticated(EMAIL, null),
                new AnonymousAuthenticationToken("test-key", "anonymousUser",
                        List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS"))));
    }
}
