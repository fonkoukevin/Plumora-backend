package com.plumora.api.admin.application;

import com.plumora.api.admin.domain.AdminAction;
import com.plumora.api.admin.domain.AdminAuditLog;
import com.plumora.api.admin.domain.AdminTargetType;
import com.plumora.api.admin.infrastructure.AdminAuditLogRepository;
import com.plumora.api.user.domain.User;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminAuditLogService {

	private static final int MAX_RESULTS = 200;

	private final AdminAuditLogRepository auditLogRepository;

	public AdminAuditLogService(AdminAuditLogRepository auditLogRepository) {
		this.auditLogRepository = auditLogRepository;
	}

	@Transactional
	public void logAction(User admin, AdminAction action, AdminTargetType targetType, UUID targetId, String description) {
		AdminAuditLog log = new AdminAuditLog();
		log.setAdmin(admin);
		log.setAdminEmail(admin.getEmail());
		log.setAction(action);
		log.setTargetType(targetType);
		log.setTargetId(targetId);
		log.setDescription(description);
		auditLogRepository.save(log);
	}

	@Transactional(readOnly = true)
	public List<AdminAuditLog> search(AdminAction action, UUID adminId, AdminTargetType targetType, LocalDateTime dateFrom, LocalDateTime dateTo) {
		return auditLogRepository.search(
			action,
			adminId,
			targetType,
			dateFrom,
			dateTo,
			PageRequest.of(0, MAX_RESULTS, Sort.by(Sort.Direction.DESC, "createdAt"))
		);
	}

	@Transactional(readOnly = true)
	public List<AdminAuditLog> getRecentActions() {
		return auditLogRepository.findTop10ByOrderByCreatedAtDesc();
	}
}
