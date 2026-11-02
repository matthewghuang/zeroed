package com.zeroed.app.dto;

import org.hibernate.validator.constraints.Length;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record RegisterRequest(
	@Email
	@NotBlank
	String email,
	@NotBlank
	@Length(max = 72)
	String password) {
}
