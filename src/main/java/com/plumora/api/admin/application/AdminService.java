package com.plumora.api.admin.application;

import com.plumora.api.admin.domain.AdminAction;
import com.plumora.api.admin.domain.AdminBookType;
import com.plumora.api.admin.domain.AdminTargetType;
import com.plumora.api.admin.presentation.AdminAiStatusDto;
import com.plumora.api.admin.presentation.AdminAuditLogMapper;
import com.plumora.api.admin.presentation.AdminBulkImportGutendexResponse;
import com.plumora.api.admin.presentation.AdminDashboardDto;
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
import com.plumora.api.book.infrastructure.gutendex.GutendexBookResponse;
import com.plumora.api.book.infrastructure.gutendex.GutendexClient;
import com.plumora.api.book.infrastructure.gutendex.GutendexPageResponse;
import com.plumora.api.book.infrastructure.gutendex.GutendexSearchRequest;
import com.plumora.api.report.application.ReportService;
import com.plumora.api.report.domain.Report;
import com.plumora.api.report.domain.ReportStatus;
import com.plumora.api.report.infrastructure.ReportRepository;
import com.plumora.api.report.presentation.UpdateReportStatusRequest;
import com.plumora.api.shared.exception.BusinessException;
import com.plumora.api.shared.exception.ResourceNotFoundException;
import com.plumora.api.user.domain.Role;
import com.plumora.api.user.domain.RoleName;
import com.plumora.api.user.domain.User;
import com.plumora.api.user.domain.UserStatus;
import com.plumora.api.user.infrastructure.RoleRepository;
import com.plumora.api.user.infrastructure.UserRepository;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class AdminService {

	private static final Logger log = LoggerFactory.getLogger(AdminService.class);

	// Hard ceiling on how many NEW books a single bulk-import call can bring in. Each import
	// downloads the book's full text from Gutenberg's own servers on top of the Gutendex API
	// call, so this stays synchronous and deliberately small per call (bulkImportGutendexBooks's
	// own doc has the full reasoning) rather than a bigger async job - a caller wanting a
	// larger batch (e.g. the 200-500 range a human operator would ask for) repeats the call
	// with the returned nextPage.
	private static final int MAX_BULK_IMPORT_LIMIT = 50;

	private final UserRepository userRepository;
	private final RoleRepository roleRepository;
	private final BookRepository bookRepository;
	private final ChapterRepository chapterRepository;
	private final ReportRepository reportRepository;
	private final ReportService reportService;
	private final ExternalBookService externalBookService;
	private final GutendexClient gutendexClient;
	private final AdminAuditLogService auditLogService;
	private final AiWritingRequestRepository aiWritingRequestRepository;
	private final AiRecommendationRequestRepository aiRecommendationRequestRepository;
	private final AiProvider aiProvider;
	private final AiFeatureToggle aiFeatureToggle;

	public AdminService(
		UserRepository userRepository,
		RoleRepository roleRepository,
		BookRepository bookRepository,
		ChapterRepository chapterRepository,
		ReportRepository reportRepository,
		ReportService reportService,
		ExternalBookService externalBookService,
		GutendexClient gutendexClient,
		AdminAuditLogService auditLogService,
		AiWritingRequestRepository aiWritingRequestRepository,
		AiRecommendationRequestRepository aiRecommendationRequestRepository,
		AiProvider aiProvider,
		AiFeatureToggle aiFeatureToggle
	) {
		this.userRepository = userRepository;
		this.roleRepository = roleRepository;
		this.bookRepository = bookRepository;
		this.chapterRepository = chapterRepository;
		this.reportRepository = reportRepository;
		this.reportService = reportService;
		this.externalBookService = externalBookService;
		this.gutendexClient = gutendexClient;
		this.auditLogService = auditLogService;
		this.aiWritingRequestRepository = aiWritingRequestRepository;
		this.aiRecommendationRequestRepository = aiRecommendationRequestRepository;
		this.aiProvider = aiProvider;
		this.aiFeatureToggle = aiFeatureToggle;
	}

	@Transactional(readOnly = true)
	public AdminDashboardDto getDashboard() {
		long aiCallsCount = aiWritingRequestRepository.count() + aiRecommendationRequestRepository.count();
		return new AdminDashboardDto(
			userRepository.count(),
			userRepository.countByActiveTrue(),
			bookRepository.count(),
			bookRepository.countByExternalSourceIsNull(),
			bookRepository.countByExternalSourceIsNotNull(),
			reportRepository.countByStatus(ReportStatus.OPEN),
			reportRepository.countByStatus(ReportStatus.RESOLVED),
			bookRepository.countByStatus(BookStatus.ARCHIVED),
			aiCallsCount,
			auditLogService.getRecentActions().stream().map(AdminAuditLogMapper::toResponse).toList()
		);
	}

	@Transactional(readOnly = true)
	public List<User> getUsers(String query, RoleName role, UserStatus status) {
		if (!StringUtils.hasText(query) && role == null && status == null) {
			return userRepository.findAllByOrderByCreatedAtDesc();
		}
		String normalizedQuery = StringUtils.hasText(query) ? "%" + query.trim().toLowerCase(Locale.ROOT) + "%" : null;
		Boolean active = status == null ? null : status == UserStatus.ACTIVE;
		return userRepository.search(normalizedQuery, role, active);
	}

	@Transactional(readOnly = true)
	public AdminUserDetail getUserDetail(UUID userId) {
		User user = findUser(userId);
		return new AdminUserDetail(user, bookRepository.countByAuthor(user), reportRepository.countByReporter(user));
	}

	@Transactional
	public User updateUserStatus(String currentAdminEmail, UUID userId, UpdateUserStatusRequest request) {
		User admin = findUser(currentAdminEmail);
		User user = findUser(userId);
		user.setActive(request.status() == UserStatus.ACTIVE);
		User saved = userRepository.save(user);
		String description = "User status set to " + request.status()
			+ (StringUtils.hasText(request.reason()) ? " (" + request.reason() + ")" : "");
		auditLogService.logAction(admin, AdminAction.USER_STATUS_UPDATED, AdminTargetType.USER, user.getId(), description);
		return saved;
	}

	@Transactional
	public User updateUserRoles(String currentAdminEmail, UUID userId, UpdateUserRoleRequest request) {
		User admin = findUser(currentAdminEmail);
		User user = findUser(userId);

		boolean losesAdminRole = user.hasRole(RoleName.ADMIN) && !request.roles().contains(RoleName.ADMIN);
		if (losesAdminRole && userRepository.countByRoles_Name(RoleName.ADMIN) <= 1) {
			throw new BusinessException("Cannot remove the ADMIN role from the last remaining administrator");
		}

		Set<Role> roles = request.roles().stream().map(this::getRole).collect(Collectors.toSet());
		user.setRoles(roles);
		User saved = userRepository.save(user);
		auditLogService.logAction(
			admin,
			AdminAction.USER_ROLE_UPDATED,
			AdminTargetType.USER,
			user.getId(),
			"Roles updated to " + request.roles()
		);
		return saved;
	}

	@Transactional
	public User disableUser(String currentAdminEmail, UUID userId) {
		User admin = findUser(currentAdminEmail);
		User user = findUser(userId);
		user.setActive(false);
		User saved = userRepository.save(user);
		auditLogService.logAction(
			admin,
			AdminAction.USER_STATUS_UPDATED,
			AdminTargetType.USER,
			user.getId(),
			"User disabled"
		);
		return saved;
	}

	@Transactional
	public User enableUser(String currentAdminEmail, UUID userId) {
		User admin = findUser(currentAdminEmail);
		User user = findUser(userId);
		user.setActive(true);
		User saved = userRepository.save(user);
		auditLogService.logAction(
			admin,
			AdminAction.USER_STATUS_UPDATED,
			AdminTargetType.USER,
			user.getId(),
			"User enabled"
		);
		return saved;
	}

	@Transactional(readOnly = true)
	public List<Book> getBooks(String query, AdminBookType type, BookStatus status) {
		if (!StringUtils.hasText(query) && type == null && status == null) {
			return bookRepository.findAllWithAuthorOrderByCreatedAtDesc();
		}
		String normalizedQuery = StringUtils.hasText(query) ? "%" + query.trim().toLowerCase(Locale.ROOT) + "%" : null;
		Boolean isExternal = type == null ? null : type == AdminBookType.PUBLIC_DOMAIN;
		return bookRepository.searchForAdmin(normalizedQuery, status, isExternal);
	}

	@Transactional(readOnly = true)
	public long getReportsCount(Book book) {
		return reportRepository.countByBook(book);
	}

	@Transactional(readOnly = true)
	public AdminBookDetail getBookDetail(UUID bookId) {
		Book book = findBook(bookId);
		return new AdminBookDetail(book, reportRepository.countByBook(book), chapterRepository.countByBook(book));
	}

	@Transactional
	public Book archiveBook(String currentAdminEmail, UUID bookId) {
		User admin = findUser(currentAdminEmail);
		Book book = findBook(bookId);
		book.setStatus(BookStatus.ARCHIVED);
		book.setVisibility(BookVisibility.PRIVATE);
		Book saved = bookRepository.save(book);
		auditLogService.logAction(
			admin,
			AdminAction.BOOK_ARCHIVED,
			AdminTargetType.BOOK,
			book.getId(),
			"Book archived: " + book.getTitle()
		);
		return saved;
	}

	@Transactional
	public Book updateBookStatus(String currentAdminEmail, UUID bookId, UpdateBookStatusRequest request) {
		User admin = findUser(currentAdminEmail);
		Book book = findBook(bookId);
		BookStatus previousStatus = book.getStatus();
		book.setStatus(request.status());
		if (request.status() == BookStatus.ARCHIVED || previousStatus == BookStatus.ARCHIVED) {
			book.setVisibility(BookVisibility.PRIVATE);
		}
		Book saved = bookRepository.save(book);

		AdminAction action = request.status() == BookStatus.ARCHIVED ? AdminAction.BOOK_ARCHIVED : AdminAction.BOOK_RESTORED;
		String description = "Book status changed from " + previousStatus + " to " + request.status()
			+ (StringUtils.hasText(request.reason()) ? " (" + request.reason() + ")" : "");
		auditLogService.logAction(admin, action, AdminTargetType.BOOK, book.getId(), description);
		return saved;
	}

	@Transactional
	public Book updateBookMetadata(String currentAdminEmail, UUID bookId, UpdateBookMetadataRequest request) {
		User admin = findUser(currentAdminEmail);
		Book book = findBook(bookId);
		if (StringUtils.hasText(request.title())) {
			book.setTitle(request.title());
		}
		if (request.authors() != null) {
			book.setExternalAuthors(request.authors());
		}
		if (request.summary() != null) {
			book.setSummary(request.summary());
		}
		if (request.subjects() != null) {
			book.setExternalSubjects(request.subjects());
		}
		if (request.languages() != null) {
			book.setExternalLanguages(request.languages());
		}
		if (StringUtils.hasText(request.coverUrl())) {
			book.setCoverUrl(request.coverUrl());
		}
		Book saved = bookRepository.save(book);
		auditLogService.logAction(
			admin,
			AdminAction.BOOK_METADATA_UPDATED,
			AdminTargetType.BOOK,
			book.getId(),
			"Metadata updated for: " + book.getTitle()
		);
		return saved;
	}

	@Transactional
	public ImportedExternalBookResult importGutendexBook(String currentAdminEmail, int gutendexId) {
		User admin = findUser(currentAdminEmail);
		ImportedExternalBookResult result = externalBookService.importGutendexBook(currentAdminEmail, gutendexId);
		String description = (result.created() ? "Imported Gutendex book " : "Gutendex book already imported ")
			+ gutendexId + ": " + result.book().getTitle();
		auditLogService.logAction(admin, AdminAction.BOOK_IMPORTED, AdminTargetType.BOOK, result.book().getId(), description);
		return result;
	}

	/**
	 * Imports up to {@code limit} (capped at {@link #MAX_BULK_IMPORT_LIMIT}) NEW public-domain
	 * Gutendex books, sorted by popularity (most downloaded first), starting from
	 * {@code startPage} of Gutendex's own search results.
	 * <p>
	 * Deliberately NOT wrapped in a single transaction and NOT made async: each book import
	 * (see {@link ExternalBookService#importGutendexBook}) already downloads that book's full
	 * text from Gutenberg's own servers and commits independently, so one book failing (a
	 * timeout, a malformed response) never rolls back the ones already imported, and the
	 * caller sees real, incremental progress rather than an opaque background job. This does
	 * mean a large ask (e.g. a few hundred books) means repeating this call several times with
	 * the {@code nextPage} this returns - see AdminBulkImportGutendexResponse's own doc.
	 */
	@Transactional(propagation = Propagation.NOT_SUPPORTED)
	public AdminBulkImportGutendexResponse bulkImportGutendexBooks(
		String currentAdminEmail,
		String language,
		int startPage,
		int limit
	) {
		User admin = findUser(currentAdminEmail);
		int safeLimit = Math.max(1, Math.min(limit, MAX_BULK_IMPORT_LIMIT));
		int page = Math.max(1, startPage);

		int scanned = 0;
		int imported = 0;
		int alreadyExisted = 0;
		int failed = 0;
		boolean hasMore = true;

		while (imported < safeLimit && hasMore) {
			GutendexPageResponse response = gutendexClient.searchBooks(
				GutendexSearchRequest.publicDomain(null, language, null, page)
			);
			if (response.results().isEmpty()) {
				hasMore = false;
				break;
			}

			for (GutendexBookResponse candidate : response.results()) {
				if (imported >= safeLimit) {
					break;
				}
				scanned++;
				try {
					ImportedExternalBookResult result = externalBookService.importGutendexBook(currentAdminEmail, candidate.id());
					if (result.created()) {
						imported++;
					} else {
						alreadyExisted++;
					}
				} catch (RuntimeException exception) {
					failed++;
					log.warn("Bulk Gutendex import: book {} failed", candidate.id(), exception);
				}
			}

			hasMore = StringUtils.hasText(response.next());
			page++;
		}

		int nextPage = hasMore ? page : page - 1;
		auditLogService.logAction(
			admin,
			AdminAction.BOOK_BULK_IMPORTED,
			AdminTargetType.BOOK,
			null,
			"Bulk Gutendex import: " + imported + " imported, " + alreadyExisted + " already present, "
				+ failed + " failed (scanned " + scanned + ", pages " + startPage + "-" + (page - 1) + ")"
		);

		return new AdminBulkImportGutendexResponse(scanned, imported, alreadyExisted, failed, nextPage, hasMore);
	}

	@Transactional(readOnly = true)
	public List<Report> getReports() {
		return reportService.getAllReports();
	}

	@Transactional(readOnly = true)
	public Report getReportDetail(UUID reportId) {
		return reportService.getReport(reportId);
	}

	@Transactional
	public Report resolveReport(String currentAdminEmail, UUID reportId, AdminReportActionRequest request) {
		User admin = findUser(currentAdminEmail);
		Report report = reportService.updateStatus(reportId, new UpdateReportStatusRequest(ReportStatus.RESOLVED));
		String description = "Report resolved"
			+ (StringUtils.hasText(request.reason()) ? " (" + request.reason() + ")" : "");
		auditLogService.logAction(admin, AdminAction.REPORT_RESOLVED, AdminTargetType.REPORT, report.getId(), description);
		return report;
	}

	@Transactional
	public Report rejectReport(String currentAdminEmail, UUID reportId, AdminReportActionRequest request) {
		User admin = findUser(currentAdminEmail);
		Report report = reportService.updateStatus(reportId, new UpdateReportStatusRequest(ReportStatus.DISMISSED));
		String description = "Report rejected"
			+ (StringUtils.hasText(request.reason()) ? " (" + request.reason() + ")" : "");
		auditLogService.logAction(admin, AdminAction.REPORT_REJECTED, AdminTargetType.REPORT, report.getId(), description);
		return report;
	}

	@Transactional(readOnly = true)
	public AdminAiStatusDto getAiStatus() {
		return new AdminAiStatusDto(
			aiFeatureToggle.isEnabled(),
			aiFeatureToggle.getUpdatedAt(),
			aiProvider.providerName(),
			aiProvider.modelName(),
			aiWritingRequestRepository.count(),
			aiRecommendationRequestRepository.count()
		);
	}

	@Transactional
	public AdminAiStatusDto updateAiSettings(String currentAdminEmail, UpdateAiSettingsRequest request) {
		User admin = findUser(currentAdminEmail);
		aiFeatureToggle.setEnabled(request.enabled());
		String description = "Plumo IA " + (request.enabled() ? "enabled" : "disabled")
			+ (StringUtils.hasText(request.reason()) ? " (" + request.reason() + ")" : "");
		auditLogService.logAction(admin, AdminAction.AI_SETTINGS_UPDATED, AdminTargetType.AI_SETTINGS, null, description);
		return getAiStatus();
	}

	private User findUser(UUID userId) {
		return userRepository.findById(userId)
			.orElseThrow(() -> new ResourceNotFoundException("User was not found"));
	}

	private User findUser(String email) {
		return userRepository.findByEmail(email)
			.orElseThrow(() -> new ResourceNotFoundException("Current user was not found"));
	}

	private Book findBook(UUID bookId) {
		return bookRepository.findByIdWithAuthor(bookId)
			.orElseThrow(() -> new ResourceNotFoundException("Book was not found"));
	}

	private Role getRole(RoleName roleName) {
		return roleRepository.findByName(roleName)
			.orElseThrow(() -> new ResourceNotFoundException("Role " + roleName + " was not found"));
	}
}
