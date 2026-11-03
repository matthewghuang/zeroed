package com.zeroed.app.services;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.zeroed.app.models.User;
import com.zeroed.app.repositories.UserRepository;

@Service 
public class CustomUserDetailsService implements UserDetailsService {
	private final UserRepository userRepository;

	public CustomUserDetailsService(UserRepository userRepository) {
		this.userRepository = userRepository;
	}

	@Override
	public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
		User user = userRepository.findByEmail(email).orElseThrow(() -> new UsernameNotFoundException("Invalid credentials"));

		return org.springframework.security.core.userdetails.User
			.withUsername(email)
			.password(user.getPasswordHash())
			.roles(user.getRole().name())
			.build();
	}
	
}
