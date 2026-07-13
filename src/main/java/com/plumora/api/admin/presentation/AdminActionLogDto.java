package com.plumora.api.admin.presentation;

import com.plumora.api.admin.domain.AdminAction;
import com.plumora.api.admin.domain.AdminTargetType;
import java.time.LocalDateTime;
import java.util.UUID;

public record AdminActionLogDto(
	UUID id,
	UUID adminId,
	String adminEmail,
	AdminAction action,
	AdminTargetType targetType,
	UUID targetId,
	String description,
	LocalDateTime createdAt
) {
}
