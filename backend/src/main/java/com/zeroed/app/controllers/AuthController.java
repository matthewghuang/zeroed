package com.zeroed.app.controllers;

import java.util.Locale;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.zeroed.app.dto.LoginRequest;
import com.zeroed.app.dto.MeResponse;
import com.zeroed.app.dto.RegisterRequest;
import com.zeroed.app.models.User;
import com.zeroed.app.services.AuthService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.GetMapping;

@RestController 
@RequestMapping("/api/auth")
public class AuthController {
	private final AuthService authService;
	private final AuthenticationManager authenticationManager;
	private final SecurityContextRepository securityContextRepository;

	public AuthController(AuthService authService, AuthenticationManager authenticationManager) {
		this.authService = authService;
		this.authenticationManager = authenticationManager;
		this.securityContextRepository = new HttpSessionSecurityContextRepository();
	}

	@PostMapping("/register")
	public ResponseEntity<Void> register(@Valid @RequestBody RegisterRequest request) {
		authService.register(request.email(), request.password());

		return ResponseEntity.status(HttpStatus.CREATED).build();
	}

	@PostMapping("/login")
	public ResponseEntity<Void> login(@Valid @RequestBody LoginRequest request,
			HttpServletRequest servletRequest, HttpServletResponse servletResponse) {
		// normalize email
		String email = request.email().trim().toLowerCase(Locale.ROOT);

		Authentication authentication;
		try {
			authentication = authenticationManager.authenticate(
				UsernamePasswordAuthenticationToken.unauthenticated(email, request.password())
			);
		} catch (AuthenticationException | IllegalArgumentException exception) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
		}

		// check if session currently exists
		if (servletRequest.getSession(false) != null) {
			servletRequest.changeSessionId();
		}

		SecurityContext context = SecurityContextHolder.createEmptyContext();

		context.setAuthentication(authentication);
		SecurityContextHolder.setContext(context);

		securityContextRepository.saveContext(context, servletRequest, servletResponse);

		return ResponseEntity.noContent().build();
	}
	
	@GetMapping("/csrf")
	public CsrfToken csrf(CsrfToken token) {
		return token;
	}

	@GetMapping("/me")
	public ResponseEntity<MeResponse> me(Authentication authentication) {
		if (authentication == null
				|| !authentication.isAuthenticated()
				|| authentication instanceof AnonymousAuthenticationToken) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
		}

		User user = authService.getCurrentUser(authentication.getName());
		return ResponseEntity.ok(new MeResponse(user.getEmail(), user.getRole()));
	}
}
