package com.plumora.api.report.presentation;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.plumora.api.book.domain.Book;
import com.plumora.api.book.domain.BookStatus;
import com.plumora.api.book.domain.BookVisibility;
import com.plumora.api.report.application.ReportService;
import com.plumora.api.report.domain.Report;
import com.plumora.api.report.domain.ReportStatus;
import com.plumora.api.shared.exception.BusinessException;
import com.plumora.api.shared.exception.DuplicateResourceException;
import com.plumora.api.shared.exception.ResourceNotFoundException;
import com.plumora.api.shared.security.CustomUserDetailsService;
import com.plumora.api.shared.security.JwtService;
import com.plumora.api.shared.security.RestAccessDeniedHandler;
import com.plumora.api.shared.security.RestAuthenticationEntryPoint;
import com.plumora.api.shared.security.SecurityConfig;
import com.plumora.api.user.domain.User;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Verifies the HTTP-level contract of the report creation and moderation routes: unauthenticated
 * requests are rejected, Bean Validation is enforced, and the not-found/duplicate business
 * exceptions map to the correct status codes. AdminController's own security is already covered
 * generically by AdminControllerSecurityTest; this class focuses on ReportController, which has
 * no dedicated HTTP-level test before this change (only the mocked-repository ReportServiceTest).
 */
@WebMvcTest(controllers = ReportController.class)
@Import({SecurityConfig.class, RestAuthenticationEntryPoint.class, RestAccessDeniedHandler.class})
class ReportControllerSecurityTest {

	private static final UUID BOOK_ID = UUID.randomUUID();

	@Autowired
	private MockMvc mockMvc;

	@MockBean
	private ReportService reportService;

	@MockBean
	private JwtService jwtService;

	@MockBean
	private CustomUserDetailsService userDetailsService;

	@Test
	void unauthenticatedRequestIsRejected() throws Exception {
		mockMvc.perform(post("/books/{bookId}/reports", BOOK_ID)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"reason\":\"Spam\"}"))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.status").value(401));

		verifyNoInteractions(reportService);
	}

	@Test
	void authenticatedUserCanCreateAReport() throws Exception {
		Report report = report(domainUser("reader@example.com"), book());
		when(reportService.createReport(eq("reader@example.com"), eq(BOOK_ID), any(CreateReportRequest.class)))
			.thenReturn(report);

		mockMvc.perform(post("/books/{bookId}/reports", BOOK_ID)
				.with(user("reader@example.com").roles("READER"))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"reason\":\"Inappropriate content\",\"description\":\"Please review\"}"))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.status").value("OPEN"))
			.andExpect(jsonPath("$.reason").value("Inappropriate content"));
	}

	@Test
	void blankReasonIsRejected() throws Exception {
		mockMvc.perform(post("/books/{bookId}/reports", BOOK_ID)
				.with(user("reader@example.com").roles("READER"))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"reason\":\"\"}"))
			.andExpect(status().isBadRequest());

		verifyNoInteractions(reportService);
	}

	@Test
	void descriptionOverTwoThousandCharactersIsRejected() throws Exception {
		String tooLong = "a".repeat(2001);

		mockMvc.perform(post("/books/{bookId}/reports", BOOK_ID)
				.with(user("reader@example.com").roles("READER"))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"reason\":\"Spam\",\"description\":\"" + tooLong + "\"}"))
			.andExpect(status().isBadRequest());

		verifyNoInteractions(reportService);
	}

	@Test
	void reportingANonExistentBookReturnsNotFound() throws Exception {
		when(reportService.createReport(eq("reader@example.com"), eq(BOOK_ID), any(CreateReportRequest.class)))
			.thenThrow(new ResourceNotFoundException("Book was not found"));

		mockMvc.perform(post("/books/{bookId}/reports", BOOK_ID)
				.with(user("reader@example.com").roles("READER"))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"reason\":\"Spam\"}"))
			.andExpect(status().isNotFound());
	}

	@Test
	void aSecondOpenReportOnTheSameBookReturnsConflict() throws Exception {
		when(reportService.createReport(eq("reader@example.com"), eq(BOOK_ID), any(CreateReportRequest.class)))
			.thenThrow(new DuplicateResourceException("You have already reported this book and it is still open"));

		mockMvc.perform(post("/books/{bookId}/reports", BOOK_ID)
				.with(user("reader@example.com").roles("READER"))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"reason\":\"Spam\"}"))
			.andExpect(status().isConflict());
	}

	@Test
	void reportingAnUnpublishableBookReturnsBadRequest() throws Exception {
		when(reportService.createReport(eq("reader@example.com"), eq(BOOK_ID), any(CreateReportRequest.class)))
			.thenThrow(new BusinessException("Only published public books can be reported"));

		mockMvc.perform(post("/books/{bookId}/reports", BOOK_ID)
				.with(user("reader@example.com").roles("READER"))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"reason\":\"Spam\"}"))
			.andExpect(status().isBadRequest());
	}

	@Test
	void nonAdminIsForbiddenFromListingAllReports() throws Exception {
		mockMvc.perform(get("/reports").with(user("reader@example.com").roles("READER")))
			.andExpect(status().isForbidden());

		verifyNoInteractions(reportService);
	}

	@Test
	void adminCanListAllReports() throws Exception {
		when(reportService.getAllReports()).thenReturn(List.of());

		mockMvc.perform(get("/reports").with(user("admin@example.com").roles("ADMIN")))
			.andExpect(status().isOk());
	}

	@Test
	void nonAdminIsForbiddenFromUpdatingReportStatus() throws Exception {
		mockMvc.perform(patch("/reports/{reportId}/status", UUID.randomUUID())
				.with(user("reader@example.com").roles("READER"))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"status\":\"RESOLVED\"}"))
			.andExpect(status().isForbidden());

		verifyNoInteractions(reportService);
	}

	@Test
	void adminCanUpdateReportStatus() throws Exception {
		UUID reportId = UUID.randomUUID();
		Report resolved = report(domainUser("reader@example.com"), book());
		resolved.setStatus(ReportStatus.RESOLVED);
		when(reportService.updateStatus(eq(reportId), any(UpdateReportStatusRequest.class))).thenReturn(resolved);

		mockMvc.perform(patch("/reports/{reportId}/status", reportId)
				.with(user("admin@example.com").roles("ADMIN"))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"status\":\"RESOLVED\"}"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value("RESOLVED"));
	}

	private static Report report(User reporter, Book book) {
		Report report = new Report();
		report.setId(UUID.randomUUID());
		report.setReporter(reporter);
		report.setBook(book);
		report.setReason("Inappropriate content");
		report.setDescription("Please review");
		report.setStatus(ReportStatus.OPEN);
		report.setCreatedAt(LocalDateTime.now());
		return report;
	}

	private static User domainUser(String email) {
		User user = new User();
		user.setId(UUID.randomUUID());
		user.setEmail(email);
		user.setUsername(email.substring(0, email.indexOf('@')));
		return user;
	}

	private static Book book() {
		Book book = new Book();
		book.setId(BOOK_ID);
		book.setAuthor(domainUser("author@example.com"));
		book.setTitle("Reported book");
		book.setGenre("Fantasy");
		book.setStatus(BookStatus.PUBLISHED);
		book.setVisibility(BookVisibility.PUBLIC);
		book.setPublishedAt(LocalDateTime.now());
		return book;
	}
}
