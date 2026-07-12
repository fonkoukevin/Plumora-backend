package com.plumora.api.admin.application;

import com.plumora.api.admin.domain.AdminAction;
import com.plumora.api.admin.domain.AdminTargetType;
import com.plumora.api.admin.presentation.AdminAuditLogMapper;
import com.plumora.api.admin.presentation.AdminDashboardDto;
import com.plumora.api.ai.infrastructure.AiRecommendationRequestRepository;
import com.plumora.api.ai.infrastructure.AiWritingRequestRepository;
import com.plumora.api.book.domain.Book;
import com.plumora.api.book.domain.BookStatus;
import com.plumora.api.book.domain.BookVisibility;
import com.plumora.api.book.infrastructure.BookRepository;
import com.plumora.api.report.application.ReportService;
import com.plumora.api.report.domain.Report;
import com.plumora.api.report.domain.ReportStatus;
import com.plumora.api.report.infrastructure.ReportRepository;
import com.plumora.api.shared.exception.ResourceNotFoundException;
import com.plumora.api.user.domain.User;
import com.plumora.api.user.infrastructure.UserRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminService {

	private final UserRepository userRepository;
	private final BookRepository bookRepository;
	private final ReportRepository reportRepository;
	private final ReportService reportService;
	private final AdminAuditLogService auditLogService;
	private final AiWritingRequestRepository aiWritingRequestRepository;
	private final AiRecommendationRequestRepository aiRecommendationRequestRepository;

	public AdminService(
		UserRepository userRepository,
		BookRepository bookRepository,
		ReportRepository reportRepository,
		ReportService reportService,
		AdminAuditLogService auditLogService,
		AiWritingRequestRepository aiWritingRequestRepository,
		AiRecommendationRequestRepository aiRecommendationRequestRepository
	) {
		this.userRepository = userRepository;
		this.bookRepository = bookRepository;
		this.reportRepository = reportRepository;
		this.reportService = reportService;
		this.auditLogService = auditLogService;
		this.aiWritingRequestRepository = aiWritingRequestRepository;
		this.aiRecommendationRequestRepository = aiRecommendationRequestRepository;
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
	public List<User> getUsers() {
		return userRepository.findAllByOrderByCreatedAtDesc();
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
	public List<Book> getBooks() {
		return bookRepository.findAllWithAuthorOrderByCreatedAtDesc();
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

	@Transactional(readOnly = true)
	public List<Report> getReports() {
		return reportService.getAllReports();
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
}
