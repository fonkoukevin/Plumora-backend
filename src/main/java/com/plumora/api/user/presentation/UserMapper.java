package com.plumora.api.user.presentation;

import com.plumora.api.user.domain.Role;
import com.plumora.api.user.domain.User;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

public final class UserMapper {
	private UserMapper() {
	}

	public static UserResponse toResponse(User user) {
		return new UserResponse(
			user.getId(),
			user.getFirstname(),
			user.getLastname(),
			user.getUsername(),
			user.getEmail(),
			user.getAvatarUrl(),
			user.getBio(),
			user.isActive(),
			toRoleResponses(user.getRoles()),
			user.getCreatedAt(),
			user.getUpdatedAt()
		);
	}

	public static RoleResponse toRoleResponse(Role role) {
		return new RoleResponse(role.getId(), role.getName(), role.getDescription());
	}

	public static UserSummaryResponse toSummaryResponse(User user) {
		return new UserSummaryResponse(user.getId(), user.getUsername());
	}

	public static Set<RoleResponse> toRoleResponses(Set<Role> roles) {
		return roles.stream()
			.sorted(Comparator.comparing(role -> role.getName().name()))
			.map(UserMapper::toRoleResponse)
			.collect(Collectors.toCollection(LinkedHashSet::new));
	}
}
