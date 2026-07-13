package com.plumora.api.admin.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.plumora.api.admin.domain.AdminAction;
import com.plumora.api.admin.domain.AdminBookType;
import com.plumora.api.admin.domain.AdminTargetType;
import com.plumora.api.admin.presentation.AdminAiStatusDto;
import com.plumora.api.admin.presentation.AdminReportActionRequest;
import com.plumora.api.admin.presentation.UpdateAiSettingsRequest;
import com.plumora.api.admin.presentation.UpdateBookMetadataRequest;
import com.plumora.api.admin.presentation.UpdateBookStatusRequest;
import com.plumora.api.admin.presentation.UpdateUserRoleRequest;
import com.plumora.api.admin.presentation.UpdateUserStatusRequest;
import com.plumora.api.ai.application.AiFeatureToggle;
import com.plumora.api.ai.infrastructure.AiRecommendationRequestRepository;
import com.plumora.api.ai.infrastructure.AiWritingRequestRepository;
import com.plumora.api.ai.infrastructure.provider.AiProvider;
import com.plumora.api.book.application.ExternalBookService;
import com.plumora.api.book.application.ImportedExternalBookResult;
import com.plumora.api.book.domain.Book;
import com.plumora.api.book.domain.BookStatus;
import com.plumora.api.book.domain.BookVisibility;
import com.plumora.api.book.infrastructure.BookRepository;
import com.plumora.api.book.infrastructure.ChapterRepository;
import com.plumora.api.report.application.ReportService;
import com.plumora.api.report.domain.Report;
import com.plumora.api.report.domain.ReportStatus;
import com.plumora.api.report.infrastructure.ReportRepository;
import com.plumora.api.report.presentation.UpdateReportStatusRequest;
import com.plumora.api.shared.exception.BusinessException;
import com.plumora.api.user.domain.Role;
import com.plumora.api.user.domain.RoleName;
import com.plumora.api.user.domain.User;
import com.plumora.api.user.domain.UserStatus;
import com.plumora.api.user.infrastructure.RoleRepository;
import com.plumora.api.user.infrastructure.UserRepository;
import java.util.List;
import java.util.Optional;
import java.util.Set;
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
	private RoleRepository roleRepository;

	@Mock
	private BookRepository bookRepository;

	@Mock
	private ChapterRepository chapterRepository;

	@Mock
	private ReportRepository reportRepository;

	@Mock
	private ReportService reportService;

	@Mock
	private ExternalBookService externalBookService;

	@Mock
	private AdminAuditLogService auditLogService;

	@Mock
	private AiWritingRequestRepository aiWritingRequestRepository;

	@Mock
	private AiRecommendationRequestRepository aiRecommendationRequestRepository;

	@Mock
	private AiProvider aiProvider;

	private final AiFeatureToggle aiFeatureToggle = new AiFeatureToggle();

	private AdminService adminService;

	@BeforeEach
	void setUp() {
		adminService = new AdminService(
			userRepository,
			roleRepository,
			bookRepository,
			chapterRepository,
			reportRepository,
			reportService,
			externalBookService,
			auditLogService,
			aiWritingRequestRepository,
			aiRecommendationRequestRepository,
			aiProvider,
			aiFeatureToggle
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

	@Test
	void getUsersWithoutFiltersReturnsFullOrderedList() {
		User user = user("reader@example.com");
		when(userRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(user));

		List<User> users = adminService.getUsers(null, null, null);

		assertThat(users).containsExactly(user);
	}

	@Test
	void getUsersWithFiltersDelegatesToSearchQuery() {
		User user = user("author@example.com");
		when(userRepository.search("%author%", RoleName.AUTHOR, true)).thenReturn(List.of(user));

		List<User> users = adminService.getUsers("author", RoleName.AUTHOR, UserStatus.ACTIVE);

		assertThat(users).containsExactly(user);
	}

	@Test
	void getUserDetailReturnsBooksAndReportsCount() {
		User user = user("author@example.com");
		when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
		when(bookRepository.countByAuthor(user)).thenReturn(5L);
		when(reportRepository.countByReporter(user)).thenReturn(2L);

		AdminUserDetail detail = adminService.getUserDetail(user.getId());

		assertThat(detail.user()).isEqualTo(user);
		assertThat(detail.booksCount()).isEqualTo(5);
		assertThat(detail.reportsCount()).isEqualTo(2);
	}

	@Test
	void updateUserStatusAppendsReasonToAuditDescription() {
		User admin = user("admin@example.com");
		User user = user("reader@example.com");
		when(userRepository.findByEmail(admin.getEmail())).thenReturn(Optional.of(admin));
		when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
		when(userRepository.save(user)).thenReturn(user);

		adminService.updateUserStatus(admin.getEmail(), user.getId(), new UpdateUserStatusRequest(UserStatus.DISABLED, "Spam"));

		assertThat(user.isActive()).isFalse();
		verify(auditLogService).logAction(
			admin,
			AdminAction.USER_STATUS_UPDATED,
			AdminTargetType.USER,
			user.getId(),
			"User status set to DISABLED (Spam)"
		);
	}

	@Test
	void adminCanChangeUserRoles() {
		User admin = user("admin@example.com");
		User user = user("reader@example.com");
		Role authorRole = new Role(RoleName.AUTHOR, "Author");
		when(userRepository.findByEmail(admin.getEmail())).thenReturn(Optional.of(admin));
		when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
		when(roleRepository.findByName(RoleName.AUTHOR)).thenReturn(Optional.of(authorRole));
		when(userRepository.save(user)).thenReturn(user);

		User updated = adminService.updateUserRoles(admin.getEmail(), user.getId(), new UpdateUserRoleRequest(Set.of(RoleName.AUTHOR)));

		assertThat(updated.getRoles()).containsExactly(authorRole);
		verify(auditLogService).logAction(
			admin,
			AdminAction.USER_ROLE_UPDATED,
			AdminTargetType.USER,
			user.getId(),
			"Roles updated to [AUTHOR]"
		);
	}

	@Test
	void cannotRemoveAdminRoleFromLastRemainingAdmin() {
		User admin = user("admin@example.com");
		admin.setRoles(Set.of(new Role(RoleName.ADMIN, "Admin")));
		when(userRepository.findByEmail(admin.getEmail())).thenReturn(Optional.of(admin));
		when(userRepository.findById(admin.getId())).thenReturn(Optional.of(admin));
		when(userRepository.countByRoles_Name(RoleName.ADMIN)).thenReturn(1L);

		assertThatThrownBy(() -> adminService.updateUserRoles(
			admin.getEmail(),
			admin.getId(),
			new UpdateUserRoleRequest(Set.of(RoleName.READER))
		))
			.isInstanceOf(BusinessException.class)
			.hasMessage("Cannot remove the ADMIN role from the last remaining administrator");
	}

	@Test
	void getBooksWithoutFiltersReturnsFullOrderedList() {
		Book book = book();
		when(bookRepository.findAllWithAuthorOrderByCreatedAtDesc()).thenReturn(List.of(book));

		List<Book> books = adminService.getBooks(null, null, null);

		assertThat(books).containsExactly(book);
	}

	@Test
	void getBooksWithFiltersDelegatesToSearchQuery() {
		Book book = book();
		when(bookRepository.searchForAdmin("%thriller%", BookStatus.PUBLISHED, false)).thenReturn(List.of(book));

		List<Book> books = adminService.getBooks("thriller", AdminBookType.PLUMORA_WORK, BookStatus.PUBLISHED);

		assertThat(books).containsExactly(book);
	}

	@Test
	void getBookDetailReturnsReportsAndChaptersCount() {
		Book book = book();
		when(bookRepository.findByIdWithAuthor(book.getId())).thenReturn(Optional.of(book));
		when(reportRepository.countByBook(book)).thenReturn(4L);
		when(chapterRepository.countByBook(book)).thenReturn(9L);

		AdminBookDetail detail = adminService.getBookDetail(book.getId());

		assertThat(detail.book()).isEqualTo(book);
		assertThat(detail.reportsCount()).isEqualTo(4);
		assertThat(detail.chaptersCount()).isEqualTo(9);
	}

	@Test
	void updateBookStatusToArchivedForcesPrivateVisibility() {
		User admin = user("admin@example.com");
		Book book = book();
		when(userRepository.findByEmail(admin.getEmail())).thenReturn(Optional.of(admin));
		when(bookRepository.findByIdWithAuthor(book.getId())).thenReturn(Optional.of(book));
		when(bookRepository.save(book)).thenReturn(book);

		Book updated = adminService.updateBookStatus(
			admin.getEmail(),
			book.getId(),
			new UpdateBookStatusRequest(BookStatus.ARCHIVED, "Copyright complaint")
		);

		assertThat(updated.getStatus()).isEqualTo(BookStatus.ARCHIVED);
		assertThat(updated.getVisibility()).isEqualTo(BookVisibility.PRIVATE);
		verify(auditLogService).logAction(
			admin,
			AdminAction.BOOK_ARCHIVED,
			AdminTargetType.BOOK,
			book.getId(),
			"Book status changed from PUBLISHED to ARCHIVED (Copyright complaint)"
		);
	}

	@Test
	void updateBookStatusRestoringFromArchivedKeepsVisibilityPrivate() {
		User admin = user("admin@example.com");
		Book book = book();
		book.setStatus(BookStatus.ARCHIVED);
		book.setVisibility(BookVisibility.PRIVATE);
		when(userRepository.findByEmail(admin.getEmail())).thenReturn(Optional.of(admin));
		when(bookRepository.findByIdWithAuthor(book.getId())).thenReturn(Optional.of(book));
		when(bookRepository.save(book)).thenReturn(book);

		Book updated = adminService.updateBookStatus(
			admin.getEmail(),
			book.getId(),
			new UpdateBookStatusRequest(BookStatus.DRAFT, null)
		);

		assertThat(updated.getStatus()).isEqualTo(BookStatus.DRAFT);
		assertThat(updated.getVisibility()).isEqualTo(BookVisibility.PRIVATE);
		verify(auditLogService).logAction(
			admin,
			AdminAction.BOOK_RESTORED,
			AdminTargetType.BOOK,
			book.getId(),
			"Book status changed from ARCHIVED to DRAFT"
		);
	}

	@Test
	void updateBookMetadataOnlyChangesProvidedFields() {
		User admin = user("admin@example.com");
		Book book = book();
		book.setSummary("Old summary");
		when(userRepository.findByEmail(admin.getEmail())).thenReturn(Optional.of(admin));
		when(bookRepository.findByIdWithAuthor(book.getId())).thenReturn(Optional.of(book));
		when(bookRepository.save(book)).thenReturn(book);

		Book updated = adminService.updateBookMetadata(
			admin.getEmail(),
			book.getId(),
			new UpdateBookMetadataRequest("New title", null, null, null, null, null)
		);

		assertThat(updated.getTitle()).isEqualTo("New title");
		assertThat(updated.getSummary()).isEqualTo("Old summary");
		verify(auditLogService).logAction(
			admin,
			AdminAction.BOOK_METADATA_UPDATED,
			AdminTargetType.BOOK,
			book.getId(),
			"Metadata updated for: New title"
		);
	}

	@Test
	void importGutendexBookLogsImportedActionWhenNewlyCreated() {
		User admin = user("admin@example.com");
		Book book = book();
		when(userRepository.findByEmail(admin.getEmail())).thenReturn(Optional.of(admin));
		when(externalBookService.importGutendexBook(admin.getEmail(), 123)).thenReturn(new ImportedExternalBookResult(book, true));

		ImportedExternalBookResult result = adminService.importGutendexBook(admin.getEmail(), 123);

		assertThat(result.created()).isTrue();
		verify(auditLogService).logAction(
			admin,
			AdminAction.BOOK_IMPORTED,
			AdminTargetType.BOOK,
			book.getId(),
			"Imported Gutendex book 123: " + book.getTitle()
		);
	}

	@Test
	void importGutendexBookLogsAlreadyImportedWhenBookExists() {
		User admin = user("admin@example.com");
		Book book = book();
		when(userRepository.findByEmail(admin.getEmail())).thenReturn(Optional.of(admin));
		when(externalBookService.importGutendexBook(admin.getEmail(), 123)).thenReturn(new ImportedExternalBookResult(book, false));

		ImportedExternalBookResult result = adminService.importGutendexBook(admin.getEmail(), 123);

		assertThat(result.created()).isFalse();
		verify(auditLogService).logAction(
			admin,
			AdminAction.BOOK_IMPORTED,
			AdminTargetType.BOOK,
			book.getId(),
			"Gutendex book already imported 123: " + book.getTitle()
		);
	}

	@Test
	void resolveReportUpdatesStatusAndLogsAction() {
		User admin = user("admin@example.com");
		Report report = report();
		when(userRepository.findByEmail(admin.getEmail())).thenReturn(Optional.of(admin));
		when(reportService.updateStatus(report.getId(), new UpdateReportStatusRequest(ReportStatus.RESOLVED)))
			.thenReturn(report);

		Report resolved = adminService.resolveReport(admin.getEmail(), report.getId(), new AdminReportActionRequest("Faux signalement"));

		assertThat(resolved).isEqualTo(report);
		verify(auditLogService).logAction(
			admin,
			AdminAction.REPORT_RESOLVED,
			AdminTargetType.REPORT,
			report.getId(),
			"Report resolved (Faux signalement)"
		);
	}

	@Test
	void rejectReportUpdatesStatusAndLogsAction() {
		User admin = user("admin@example.com");
		Report report = report();
		when(userRepository.findByEmail(admin.getEmail())).thenReturn(Optional.of(admin));
		when(reportService.updateStatus(report.getId(), new UpdateReportStatusRequest(ReportStatus.DISMISSED)))
			.thenReturn(report);

		Report rejected = adminService.rejectReport(admin.getEmail(), report.getId(), new AdminReportActionRequest(null));

		assertThat(rejected).isEqualTo(report);
		verify(auditLogService).logAction(
			admin,
			AdminAction.REPORT_REJECTED,
			AdminTargetType.REPORT,
			report.getId(),
			"Report rejected"
		);
	}

	@Test
	void getAiStatusReflectsToggleAndProviderInfo() {
		when(aiProvider.providerName()).thenReturn("gemini");
		when(aiProvider.modelName()).thenReturn("gemini-flash-lite-latest");
		when(aiWritingRequestRepository.count()).thenReturn(3L);
		when(aiRecommendationRequestRepository.count()).thenReturn(2L);

		AdminAiStatusDto status = adminService.getAiStatus();

		assertThat(status.enabled()).isTrue();
		assertThat(status.providerName()).isEqualTo("gemini");
		assertThat(status.modelName()).isEqualTo("gemini-flash-lite-latest");
		assertThat(status.totalWritingRequests()).isEqualTo(3);
		assertThat(status.totalRecommendationRequests()).isEqualTo(2);
	}

	@Test
	void updateAiSettingsTogglesFeatureAndLogsAction() {
		User admin = user("admin@example.com");
		when(userRepository.findByEmail(admin.getEmail())).thenReturn(Optional.of(admin));

		AdminAiStatusDto status = adminService.updateAiSettings(
			admin.getEmail(),
			new UpdateAiSettingsRequest(false, "Maintenance")
		);

		assertThat(status.enabled()).isFalse();
		assertThat(aiFeatureToggle.isEnabled()).isFalse();
		verify(auditLogService).logAction(
			admin,
			AdminAction.AI_SETTINGS_UPDATED,
			AdminTargetType.AI_SETTINGS,
			null,
			"Plumo IA disabled (Maintenance)"
		);
	}

	private Report report() {
		Report report = new Report();
		report.setId(UUID.randomUUID());
		report.setReporter(user("reader@example.com"));
		report.setBook(book());
		report.setReason("Contenu inapproprie");
		report.setStatus(ReportStatus.OPEN);
		return report;
	}

	private Book book() {
		Book book = new Book();
		book.setId(UUID.randomUUID());
		book.setAuthor(user("author@example.com"));
		book.setTitle("Problematic book");
		book.setGenre("Thriller");
		book.setStatus(BookStatus.PUBLISHED);
		book.setVisibility(BookVisibility.PUBLIC);
		return book;
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
