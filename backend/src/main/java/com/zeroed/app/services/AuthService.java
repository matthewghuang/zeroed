package com.zeroed.app.services;

import java.util.Locale;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.zeroed.app.models.User;
import com.zeroed.app.repositories.UserRepository;

@Service 
public class AuthService {
	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;
	
	public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
		this.userRepository = userRepository;
		this.passwordEncoder = passwordEncoder;
	}

	public void register(String _email, String rawPassword) {
		String normalizedEmail = _email.trim().toLowerCase(Locale.ROOT);

		if (userRepository.existsByEmail(normalizedEmail)) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already registered");
		}

		String hash = passwordEncoder.encode(rawPassword);

		User user = new User(normalizedEmail, hash);
		userRepository.save(user);
	}
}
