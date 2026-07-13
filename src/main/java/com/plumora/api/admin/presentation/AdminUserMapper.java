package com.plumora.api.admin.presentation;

import com.plumora.api.admin.application.AdminUserDetail;
import com.plumora.api.user.domain.User;
import com.plumora.api.user.domain.UserStatus;
import com.plumora.api.user.presentation.UserMapper;

public final class AdminUserMapper {
	private AdminUserMapper() {
	}

	public static AdminUserListDto toListDto(User user) {
		return new AdminUserListDto(
			user.getId(),
			user.getUsername(),
			user.getEmail(),
			UserMapper.toRoleResponses(user.getRoles()),
			status(user),
			user.getCreatedAt()
		);
	}

	public static AdminUserDetailDto toDetailDto(AdminUserDetail detail) {
		User user = detail.user();
		return new AdminUserDetailDto(
			user.getId(),
			user.getUsername(),
			user.getEmail(),
			UserMapper.toRoleResponses(user.getRoles()),
			status(user),
			user.getCreatedAt(),
			user.getUpdatedAt(),
			detail.booksCount(),
			detail.reportsCount()
		);
	}

	private static UserStatus status(User user) {
		return user.isActive() ? UserStatus.ACTIVE : UserStatus.DISABLED;
	}
}
