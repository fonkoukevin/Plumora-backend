package com.plumora.api.admin.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.plumora.api.admin.domain.AdminAction;
import com.plumora.api.admin.domain.AdminAuditLog;
import com.plumora.api.admin.domain.AdminTargetType;
import com.plumora.api.admin.infrastructure.AdminAuditLogRepository;
import com.plumora.api.user.domain.User;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class AdminAuditLogServiceTest {

	@Mock
	private AdminAuditLogRepository auditLogRepository;

	private AdminAuditLogService auditLogService;

	@BeforeEach
	void setUp() {
		auditLogService = new AdminAuditLogService(auditLogRepository);
	}

	@Test
	void logActionSavesAnEntryWithAdminAndDescription() {
		User admin = user("admin@example.com");
		UUID targetId = UUID.randomUUID();
		ArgumentCaptor<AdminAuditLog> captor = ArgumentCaptor.forClass(AdminAuditLog.class);
		when(auditLogRepository.save(captor.capture())).thenAnswer(invocation -> invocation.getArgument(0));

		auditLogService.logAction(admin, AdminAction.BOOK_ARCHIVED, AdminTargetType.BOOK, targetId, "Book archived: Test");

		AdminAuditLog saved = captor.getValue();
		assertThat(saved.getAdmin()).isEqualTo(admin);
		assertThat(saved.getAdminEmail()).isEqualTo("admin@example.com");
		assertThat(saved.getAction()).isEqualTo(AdminAction.BOOK_ARCHIVED);
		assertThat(saved.getTargetType()).isEqualTo(AdminTargetType.BOOK);
		assertThat(saved.getTargetId()).isEqualTo(targetId);
		assertThat(saved.getDescription()).isEqualTo("Book archived: Test");
	}

	@Test
	void searchDelegatesFiltersToRepository() {
		when(auditLogRepository.search(
			eq(AdminAction.USER_ROLE_UPDATED),
			isNull(),
			isNull(),
			isNull(),
			isNull(),
			any(Pageable.class)
		)).thenReturn(List.of());

		List<AdminAuditLog> results = auditLogService.search(AdminAction.USER_ROLE_UPDATED, null, null, null, null);

		assertThat(results).isEmpty();
	}

	private User user(String email) {
		User user = new User();
		user.setId(UUID.randomUUID());
		user.setEmail(email);
		user.setUsername("admin");
		return user;
	}
}
