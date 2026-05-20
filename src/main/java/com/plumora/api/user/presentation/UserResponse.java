package com.plumora.api.user.presentation;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

public record UserResponse(
	UUID id,
	String firstname,
	String lastname,
	String username,
	String email,
	String avatarUrl,
	String bio,
	boolean active,
	Set<RoleResponse> roles,
	LocalDateTime createdAt,
	LocalDateTime updatedAt
) {
}
