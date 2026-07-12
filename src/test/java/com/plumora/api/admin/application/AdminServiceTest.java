package com.plumora.api.admin.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.plumora.api.admin.domain.AdminAction;
import com.plumora.api.admin.domain.AdminTargetType;
import com.plumora.api.book.domain.Book;
import com.plumora.api.book.domain.BookStatus;
import com.plumora.api.book.domain.BookVisibility;
import com.plumora.api.book.infrastructure.BookRepository;
import com.plumora.api.ai.infrastructure.AiRecommendationRequestRepository;
import com.plumora.api.ai.infrastructure.AiWritingRequestRepository;
import com.plumora.api.report.application.ReportService;
import com.plumora.api.report.domain.ReportStatus;
import com.plumora.api.report.infrastructure.ReportRepository;
import com.plumora.api.user.domain.User;
import com.plumora.api.user.infrastructure.UserRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AdminServiceTest {

	@Mock
	private UserRepository userRepository;

	@Mock
	private BookRepository bookRepository;

	@Mock
	private ReportRepository reportRepository;

	@Mock
	private ReportService reportService;

	@Mock
	private AdminAuditLogService auditLogService;

	@Mock
	private AiWritingRequestRepository aiWritingRequestRepository;

	@Mock
	private AiRecommendationRequestRepository aiRecommendationRequestRepository;

	private AdminService adminService;

	@BeforeEach
	void setUp() {
		adminService = new AdminService(
			userRepository,
			bookRepository,
			reportRepository,
			reportService,
			auditLogService,
			aiWritingRequestRepository,
			aiRecommendationRequestRepository
		);
	}

	@Test
	void adminCanDisableAndEnableUser() {
		User admin = user("admin@example.com");
		User user = user("reader@example.com");
		when(userRepository.findByEmail(admin.getEmail())).thenReturn(Optional.of(admin));
		when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
		when(userRepository.save(user)).thenReturn(user);

		User disabled = adminService.disableUser(admin.getEmail(), user.getId());
		assertThat(disabled.isActive()).isFalse();
		verify(auditLogService).logAction(admin, AdminAction.USER_STATUS_UPDATED, AdminTargetType.USER, user.getId(), "User disabled");

		User enabled = adminService.enableUser(admin.getEmail(), user.getId());
		assertThat(enabled.isActive()).isTrue();
		verify(auditLogService).logAction(admin, AdminAction.USER_STATUS_UPDATED, AdminTargetType.USER, user.getId(), "User enabled");
	}

	@Test
	void adminArchiveBookMakesItPrivate() {
		User admin = user("admin@example.com");
		Book book = new Book();
		book.setId(UUID.randomUUID());
		book.setAuthor(user("author@example.com"));
		book.setTitle("Problematic book");
		book.setGenre("Thriller");
		book.setStatus(BookStatus.PUBLISHED);
		book.setVisibility(BookVisibility.PUBLIC);

		when(userRepository.findByEmail(admin.getEmail())).thenReturn(Optional.of(admin));
		when(bookRepository.findByIdWithAuthor(book.getId())).thenReturn(Optional.of(book));
		when(bookRepository.save(book)).thenReturn(book);

		Book archived = adminService.archiveBook(admin.getEmail(), book.getId());

		assertThat(archived.getStatus()).isEqualTo(BookStatus.ARCHIVED);
		assertThat(archived.getVisibility()).isEqualTo(BookVisibility.PRIVATE);
		verify(auditLogService).logAction(
			admin,
			AdminAction.BOOK_ARCHIVED,
			AdminTargetType.BOOK,
			book.getId(),
			"Book archived: Problematic book"
		);
	}

	@Test
	void dashboardAggregatesCountsFromEveryRepository() {
		when(userRepository.count()).thenReturn(42L);
		when(userRepository.countByActiveTrue()).thenReturn(40L);
		when(bookRepository.count()).thenReturn(15L);
		when(bookRepository.countByExternalSourceIsNull()).thenReturn(10L);
		when(bookRepository.countByExternalSourceIsNotNull()).thenReturn(5L);
		when(reportRepository.countByStatus(ReportStatus.OPEN)).thenReturn(3L);
		when(reportRepository.countByStatus(ReportStatus.RESOLVED)).thenReturn(7L);
		when(bookRepository.countByStatus(BookStatus.ARCHIVED)).thenReturn(2L);
		when(aiWritingRequestRepository.count()).thenReturn(6L);
		when(aiRecommendationRequestRepository.count()).thenReturn(4L);
		when(auditLogService.getRecentActions()).thenReturn(List.of());

		var dashboard = adminService.getDashboard();

		assertThat(dashboard.totalUsers()).isEqualTo(42);
		assertThat(dashboard.activeUsers()).isEqualTo(40);
		assertThat(dashboard.totalBooks()).isEqualTo(15);
		assertThat(dashboard.plumoraBooks()).isEqualTo(10);
		assertThat(dashboard.publicDomainBooks()).isEqualTo(5);
		assertThat(dashboard.pendingReports()).isEqualTo(3);
		assertThat(dashboard.resolvedReports()).isEqualTo(7);
		assertThat(dashboard.archivedBooks()).isEqualTo(2);
		assertThat(dashboard.aiCallsCount()).isEqualTo(10);
		assertThat(dashboard.recentAdminActions()).isEmpty();
	}

	private User user(String email) {
		User user = new User();
		user.setId(UUID.randomUUID());
		user.setEmail(email);
		user.setUsername(email.substring(0, email.indexOf('@')));
		user.setActive(true);
		return user;
	}
}
