package com.plumora.api.report.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

	@Mock
	private ReportRepository reportRepository;

	@Mock
	private BookRepository bookRepository;

	@Mock
	private UserService userService;

	private ReportService reportService;

	@BeforeEach
	void setUp() {
		reportService = new ReportService(reportRepository, bookRepository, userService);
	}

	@Test
	void authenticatedUserCanReportPublishedPublicBook() {
		User reporter = user("reader@example.com");
		Book book = book(BookStatus.PUBLISHED, BookVisibility.PUBLIC);

		when(userService.getCurrentUser(reporter.getEmail())).thenReturn(reporter);
		when(bookRepository.findByIdWithAuthor(book.getId())).thenReturn(Optional.of(book));
		when(reportRepository.save(any(Report.class))).thenAnswer(invocation -> {
			Report report = invocation.getArgument(0);
			report.setId(UUID.randomUUID());
			return report;
		});

		Report report = reportService.createReport(
			reporter.getEmail(),
			book.getId(),
			new CreateReportRequest("Contenu a verifier", "Description")
		);

		assertThat(report.getReporter()).isEqualTo(reporter);
		assertThat(report.getBook()).isEqualTo(book);
		assertThat(report.getStatus()).isEqualTo(ReportStatus.OPEN);
	}

	@Test
	void reportingANonExistentBookFails() {
		User reporter = user("reader@example.com");
		UUID missingBookId = UUID.randomUUID();

		when(userService.getCurrentUser(reporter.getEmail())).thenReturn(reporter);
		when(bookRepository.findByIdWithAuthor(missingBookId)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> reportService.createReport(
			reporter.getEmail(),
			missingBookId,
			new CreateReportRequest("Spam", null)
		))
			.isInstanceOf(ResourceNotFoundException.class);
	}

	@Test
	void aSecondOpenReportFromTheSameUserOnTheSameBookIsRejected() {
		User reporter = user("reader@example.com");
		Book book = book(BookStatus.PUBLISHED, BookVisibility.PUBLIC);

		when(userService.getCurrentUser(reporter.getEmail())).thenReturn(reporter);
		when(bookRepository.findByIdWithAuthor(book.getId())).thenReturn(Optional.of(book));
		when(reportRepository.existsByReporterAndBookAndStatus(reporter, book, ReportStatus.OPEN)).thenReturn(true);

		assertThatThrownBy(() -> reportService.createReport(
			reporter.getEmail(),
			book.getId(),
			new CreateReportRequest("Spam", null)
		))
			.isInstanceOf(DuplicateResourceException.class)
			.hasMessage("You have already reported this book and it is still open");

		verify(reportRepository, never()).save(any(Report.class));
	}

	@Test
	void draftBookCannotBeReported() {
		User reporter = user("reader@example.com");
		Book book = book(BookStatus.DRAFT, BookVisibility.PRIVATE);

		when(userService.getCurrentUser(reporter.getEmail())).thenReturn(reporter);
		when(bookRepository.findByIdWithAuthor(book.getId())).thenReturn(Optional.of(book));

		assertThatThrownBy(() -> reportService.createReport(
			reporter.getEmail(),
			book.getId(),
			new CreateReportRequest("Spam", null)
		))
			.isInstanceOf(BusinessException.class)
			.hasMessage("Only published public books can be reported");
	}

	@Test
	void userCanListOwnReports() {
		User reporter = user("reader@example.com");
		Report report = report(reporter, book(BookStatus.PUBLISHED, BookVisibility.PUBLIC));

		when(userService.getCurrentUser(reporter.getEmail())).thenReturn(reporter);
		when(reportRepository.findByReporterOrderByCreatedAtDesc(reporter)).thenReturn(List.of(report));

		assertThat(reportService.getMyReports(reporter.getEmail())).containsExactly(report);
	}

	@Test
	void adminStatusUpdateSetsResolvedAtForTerminalStatuses() {
		Report report = report(user("reader@example.com"), book(BookStatus.PUBLISHED, BookVisibility.PUBLIC));

		when(reportRepository.findByIdWithReporterAndBook(report.getId())).thenReturn(Optional.of(report));
		when(reportRepository.save(report)).thenReturn(report);

		Report updated = reportService.updateStatus(report.getId(), new UpdateReportStatusRequest(ReportStatus.RESOLVED));

		assertThat(updated.getStatus()).isEqualTo(ReportStatus.RESOLVED);
		assertThat(updated.getResolvedAt()).isNotNull();
		verify(reportRepository).save(report);
	}

	private Report report(User reporter, Book book) {
		Report report = new Report();
		report.setId(UUID.randomUUID());
		report.setReporter(reporter);
		report.setBook(book);
		report.setReason("Reason");
		report.setStatus(ReportStatus.OPEN);
		report.setCreatedAt(LocalDateTime.now());
		return report;
	}

	private Book book(BookStatus status, BookVisibility visibility) {
		Book book = new Book();
		book.setId(UUID.randomUUID());
		book.setAuthor(user("author@example.com"));
		book.setTitle("Book");
		book.setGenre("Fantasy");
		book.setStatus(status);
		book.setVisibility(visibility);
		if (status == BookStatus.PUBLISHED) {
			book.setPublishedAt(LocalDateTime.now());
		}
		return book;
	}

	private User user(String email) {
		User user = new User();
		user.setId(UUID.randomUUID());
		user.setEmail(email);
		user.setUsername(email.substring(0, email.indexOf('@')));
		return user;
	}
}
