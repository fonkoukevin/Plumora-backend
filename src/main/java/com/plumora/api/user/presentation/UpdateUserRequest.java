package com.plumora.api.user.presentation;

import jakarta.validation.constraints.Size;

public record UpdateUserRequest(
	@Size(max = 80) String firstname,
	@Size(max = 80) String lastname,
	@Size(max = 50) String username,
	@Size(max = 500) String avatarUrl,
	@Size(max = 2000) String bio
) {
}
