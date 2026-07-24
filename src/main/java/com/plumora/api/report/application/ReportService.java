package com.plumora.api.report.application;

import com.plumora.api.book.domain.Book;
import com.plumora.api.book.domain.BookStatus;
import com.plumora.api.book.domain.BookVisibility;
import com.plumora.api.book.infrastructure.BookRepository;
import com.plumora.api.report.domain.Report;
import com.plumora.api.report.domain.ReportStatus;
import com.plumora.api.report.infrastructure.ReportRepository;
import com.plumora.api.report.presentation.CreateReportRequest;
import com.plumora.api.report.presentation.UpdateReportStatusRequest;
import com.plumora.api.shared.exception.BusinessException;
import com.plumora.api.shared.exception.DuplicateResourceException;
import com.plumora.api.shared.exception.ResourceNotFoundException;
import com.plumora.api.user.application.UserService;
import com.plumora.api.user.domain.User;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReportService {

	private final ReportRepository reportRepository;
	private final BookRepository bookRepository;
	private final UserService userService;

	public ReportService(
		ReportRepository reportRepository,
		BookRepository bookRepository,
		UserService userService
	) {
		this.reportRepository = reportRepository;
		this.bookRepository = bookRepository;
		this.userService = userService;
	}

	@Transactional
	public Report createReport(String currentUserEmail, UUID bookId, CreateReportRequest request) {
		User reporter = userService.getCurrentUser(currentUserEmail);
		Book book = findBook(bookId);
		ensureReportable(book);
		ensureNoOpenDuplicate(reporter, book);

		Report report = new Report();
		report.setReporter(reporter);
		report.setBook(book);
		report.setReason(request.reason());
		report.setDescription(request.description());
		report.setStatus(ReportStatus.OPEN);
		return reportRepository.save(report);
	}

	@Transactional(readOnly = true)
	public List<Report> getMyReports(String currentUserEmail) {
		User reporter = userService.getCurrentUser(currentUserEmail);
		return reportRepository.findByReporterOrderByCreatedAtDesc(reporter);
	}

	@Transactional(readOnly = true)
	public List<Report> getAllReports() {
		return reportRepository.findAllByOrderByCreatedAtDesc();
	}

	@Transactional(readOnly = true)
	public Report getReport(UUID reportId) {
		return findReport(reportId);
	}

	@Transactional
	public Report updateStatus(UUID reportId, UpdateReportStatusRequest request) {
		Report report = findReport(reportId);
		report.setStatus(request.status());
		if (request.status() == ReportStatus.RESOLVED || request.status() == ReportStatus.DISMISSED) {
			report.setResolvedAt(LocalDateTime.now());
		} else {
			report.setResolvedAt(null);
		}
		return reportRepository.save(report);
	}

	private Report findReport(UUID reportId) {
		return reportRepository.findByIdWithReporterAndBook(reportId)
			.orElseThrow(() -> new ResourceNotFoundException("Report was not found"));
	}

	private Book findBook(UUID bookId) {
		return bookRepository.findByIdWithAuthor(bookId)
			.orElseThrow(() -> new ResourceNotFoundException("Book was not found"));
	}

	private void ensureReportable(Book book) {
		if (
			book.getStatus() != BookStatus.PUBLISHED
				|| book.getVisibility() != BookVisibility.PUBLIC
				|| book.getPublishedAt() == null
		) {
			throw new BusinessException("Only published public books can be reported");
		}
	}

	private void ensureNoOpenDuplicate(User reporter, Book book) {
		if (reportRepository.existsByReporterAndBookAndStatus(reporter, book, ReportStatus.OPEN)) {
			throw new DuplicateResourceException("You have already reported this book and it is still open");
		}
	}
}
