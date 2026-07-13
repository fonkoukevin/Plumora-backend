package com.plumora.api.admin.presentation;

import com.plumora.api.user.domain.UserStatus;
import com.plumora.api.user.presentation.RoleResponse;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

public record AdminUserDetailDto(
	UUID id,
	String username,
	String email,
	Set<RoleResponse> roles,
	UserStatus status,
	LocalDateTime createdAt,
	LocalDateTime updatedAt,
	long booksCount,
	long reportsCount
) {
}
