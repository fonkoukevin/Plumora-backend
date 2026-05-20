package com.plumora.api.user.presentation;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
	@NotBlank @Size(max = 80) String firstname,
	@NotBlank @Size(max = 80) String lastname,
	@NotBlank @Size(min = 3, max = 50) String username,
	@NotBlank @Email @Size(max = 150) String email,
	@NotBlank @Size(min = 8, max = 100) String password
) {
}
