package com.plumora.api.book.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.plumora.api.book.domain.Book;
import com.plumora.api.book.domain.BookStatus;
import com.plumora.api.book.domain.BookVisibility;
import com.plumora.api.book.infrastructure.BookRepository;
import com.plumora.api.book.infrastructure.ChapterRepository;
import com.plumora.api.book.presentation.CreateBookRequest;
import com.plumora.api.shared.exception.BusinessException;
import com.plumora.api.shared.exception.UnauthorizedActionException;
import com.plumora.api.user.application.UserService;
import com.plumora.api.user.domain.User;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BookServiceTest {

	@Mock
	private BookRepository bookRepository;

	@Mock
	private ChapterRepository chapterRepository;

	@Mock
	private UserService userService;

	private BookService bookService;

	@BeforeEach
	void setUp() {
		bookService = new BookService(bookRepository, chapterRepository, userService);
	}

	@Test
	void createBookStartsAsDraftAndPrivate() {
		User author = user("author@example.com");
		CreateBookRequest request = new CreateBookRequest(
			"Book title",
			"Subtitle",
			"Summary",
			"https://example.com/cover.jpg",
			"Fantasy",
			null
		);

		when(userService.getCurrentUser(author.getEmail())).thenReturn(author);
		when(bookRepository.save(any(Book.class))).thenAnswer(invocation -> {
			Book book = invocation.getArgument(0);
			book.setId(UUID.randomUUID());
			return book;
		});

		Book book = bookService.createBook(author.getEmail(), request);

		assertThat(book.getAuthor()).isEqualTo(author);
		assertThat(book.getTitle()).isEqualTo("Book title");
		assertThat(book.getLanguageCode()).isEqualTo("fr");
		assertThat(book.getStatus()).isEqualTo(BookStatus.DRAFT);
		assertThat(book.getVisibility()).isEqualTo(BookVisibility.PRIVATE);
		assertThat(book.getPublishedAt()).isNull();
	}

	@Test
	void publishBookSetsPublishedPublicAndPublishedAt() {
		Book book = book(user("author@example.com"));
		when(bookRepository.findById(book.getId())).thenReturn(Optional.of(book));
		when(chapterRepository.countByBook(book)).thenReturn(1L);
		when(bookRepository.save(book)).thenReturn(book);

		Book published = bookService.publishBook("author@example.com", book.getId());

		assertThat(published.getStatus()).isEqualTo(BookStatus.PUBLISHED);
		assertThat(published.getVisibility()).isEqualTo(BookVisibility.PUBLIC);
		assertThat(published.getPublishedAt()).isNotNull();
	}

	@Test
	void unauthorizedUserCannotPublishAnotherAuthorsBook() {
		Book book = book(user("author@example.com"));
		when(bookRepository.findById(book.getId())).thenReturn(Optional.of(book));

		assertThatThrownBy(() -> bookService.publishBook("other@example.com", book.getId()))
			.isInstanceOf(UnauthorizedActionException.class)
			.hasMessage("Only the author can manage this book");
	}

	@Test
	void cannotPublishBookWithoutChapters() {
		Book book = book(user("author@example.com"));
		when(bookRepository.findById(book.getId())).thenReturn(Optional.of(book));
		when(chapterRepository.countByBook(book)).thenReturn(0L);

		assertThatThrownBy(() -> bookService.publishBook("author@example.com", book.getId()))
			.isInstanceOf(BusinessException.class)
			.hasMessage("A book must have at least one chapter before publication");
	}

	private User user(String email) {
		User user = new User();
		user.setId(UUID.randomUUID());
		user.setEmail(email);
		user.setUsername(email.substring(0, email.indexOf('@')));
		return user;
	}

	private Book book(User author) {
		Book book = new Book();
		book.setId(UUID.randomUUID());
		book.setAuthor(author);
		book.setTitle("Book title");
		book.setGenre("Fantasy");
		book.setStatus(BookStatus.DRAFT);
		book.setVisibility(BookVisibility.PRIVATE);
		return book;
	}
}
