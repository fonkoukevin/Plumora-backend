package com.plumora.api.admin.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.plumora.api.book.domain.Book;
import com.plumora.api.book.domain.BookStatus;
import com.plumora.api.book.domain.BookVisibility;
import com.plumora.api.book.infrastructure.BookRepository;
import com.plumora.api.report.application.ReportService;
import com.plumora.api.user.domain.User;
import com.plumora.api.user.infrastructure.UserRepository;
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
	private ReportService reportService;

	private AdminService adminService;

	@BeforeEach
	void setUp() {
		adminService = new AdminService(userRepository, bookRepository, reportService);
	}

	@Test
	void adminCanDisableAndEnableUser() {
		User user = user("reader@example.com");
		when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
		when(userRepository.save(user)).thenReturn(user);

		User disabled = adminService.disableUser(user.getId());
		assertThat(disabled.isActive()).isFalse();

		User enabled = adminService.enableUser(user.getId());
		assertThat(enabled.isActive()).isTrue();
	}

	@Test
	void adminArchiveBookMakesItPrivate() {
		Book book = new Book();
		book.setId(UUID.randomUUID());
		book.setAuthor(user("author@example.com"));
		book.setTitle("Problematic book");
		book.setGenre("Thriller");
		book.setStatus(BookStatus.PUBLISHED);
		book.setVisibility(BookVisibility.PUBLIC);

		when(bookRepository.findByIdWithAuthor(book.getId())).thenReturn(Optional.of(book));
		when(bookRepository.save(book)).thenReturn(book);

		Book archived = adminService.archiveBook(book.getId());

		assertThat(archived.getStatus()).isEqualTo(BookStatus.ARCHIVED);
		assertThat(archived.getVisibility()).isEqualTo(BookVisibility.PRIVATE);
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
