package com.plumora.api.admin.presentation;

import com.plumora.api.admin.domain.AdminAuditLog;

public final class AdminAuditLogMapper {
	private AdminAuditLogMapper() {
	}

	public static AdminActionLogDto toResponse(AdminAuditLog log) {
		return new AdminActionLogDto(
			log.getId(),
			log.getAdmin().getId(),
			log.getAdminEmail(),
			log.getAction(),
			log.getTargetType(),
			log.getTargetId(),
			log.getDescription(),
			log.getCreatedAt()
		);
	}
}
