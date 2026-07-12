package com.plumora.api.book.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.plumora.api.book.domain.Book;
import com.plumora.api.book.domain.BookStatus;
import com.plumora.api.book.domain.BookVisibility;
import com.plumora.api.book.infrastructure.BookChapterStats;
import com.plumora.api.book.infrastructure.BookRepository;
import com.plumora.api.book.infrastructure.ChapterRepository;
import com.plumora.api.book.presentation.CreateBookRequest;
import com.plumora.api.book.presentation.UpdateBookRequest;
import com.plumora.api.shared.exception.BusinessException;
import com.plumora.api.shared.exception.UnauthorizedActionException;
import com.plumora.api.user.application.UserService;
import com.plumora.api.user.domain.User;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;

@ExtendWith(MockitoExtension.class)
class BookServiceTest {

	@Mock
	private BookRepository bookRepository;

	@Mock
	private ChapterRepository chapterRepository;

	@Mock
	private UserService userService;

	@Mock
	private BookCoverStorage bookCoverStorage;

	private BookService bookService;

	@BeforeEach
	void setUp() {
		bookService = new BookService(bookRepository, chapterRepository, userService, bookCoverStorage);
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
		assertThat(book.getCoverUrl()).isEqualTo("https://example.com/cover.jpg");
		assertThat(book.getLanguageCode()).isEqualTo("fr");
		assertThat(book.getStatus()).isEqualTo(BookStatus.DRAFT);
		assertThat(book.getVisibility()).isEqualTo(BookVisibility.PRIVATE);
		assertThat(book.getPublishedAt()).isNull();
	}

	@Test
	void createBookStoresUploadedCoverImage() {
		User author = user("author@example.com");
		CreateBookRequest request = new CreateBookRequest(
			"Book title",
			null,
			"Summary",
			null,
			"Fantasy",
			"fr"
		);
		MockMultipartFile coverImage = new MockMultipartFile(
			"coverImage",
			"cover.png",
			MediaType.IMAGE_PNG_VALUE,
			new byte[] {1, 2, 3}
		);

		when(userService.getCurrentUser(author.getEmail())).thenReturn(author);
		when(bookCoverStorage.store(coverImage)).thenReturn("uploads/book-covers/cover.png");
		when(bookRepository.save(any(Book.class))).thenAnswer(invocation -> {
			Book book = invocation.getArgument(0);
			book.setId(UUID.randomUUID());
			return book;
		});

		Book book = bookService.createBook(author.getEmail(), request, coverImage);

		assertThat(book.getCoverUrl()).isEqualTo("uploads/book-covers/cover.png");
	}

	@Test
	void multipartUpdateWithoutCoverKeepsExistingCoverUrl() {
		Book book = book(user("author@example.com"));
		book.setCoverUrl("uploads/book-covers/existing.png");
		UpdateBookRequest request = new UpdateBookRequest(
			"Updated title",
			null,
			"Updated summary",
			null,
			"Fantasy",
			"fr"
		);

		when(bookRepository.findById(book.getId())).thenReturn(Optional.of(book));
		when(bookRepository.save(book)).thenReturn(book);

		Book updated = bookService.updateBook("author@example.com", book.getId(), request, null);

		assertThat(updated.getTitle()).isEqualTo("Updated title");
		assertThat(updated.getCoverUrl()).isEqualTo("uploads/book-covers/existing.png");
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

	@Test
	void archiveBookMakesBookPrivate() {
		Book book = book(user("author@example.com"));
		book.setStatus(BookStatus.PUBLISHED);
		book.setVisibility(BookVisibility.PUBLIC);

		when(bookRepository.findById(book.getId())).thenReturn(Optional.of(book));
		when(bookRepository.save(book)).thenReturn(book);

		Book archived = bookService.archiveBook("author@example.com", book.getId());

		assertThat(archived.getStatus()).isEqualTo(BookStatus.ARCHIVED);
		assertThat(archived.getVisibility()).isEqualTo(BookVisibility.PRIVATE);
	}

	@Test
	void getChapterStatsForSingleBookSumsWordCount() {
		Book book = book(user("author@example.com"));
		when(chapterRepository.countByBook(book)).thenReturn(3L);
		when(chapterRepository.sumWordCountByBook(book)).thenReturn(450L);

		ChapterStats stats = bookService.getChapterStats(book);

		assertThat(stats.chapterCount()).isEqualTo(3);
		assertThat(stats.wordCount()).isEqualTo(450);
	}

	@Test
	void getChapterStatsForBookListGroupsByBookAndDefaultsMissingBooks() {
		Book bookWithChapters = book(user("author@example.com"));
		Book bookWithoutChapters = book(user("author@example.com"));
		BookChapterStats rawStats = bookChapterStats(bookWithChapters.getId(), 2L, 300L);

		when(chapterRepository.findStatsByBooks(List.of(bookWithChapters, bookWithoutChapters))).thenReturn(List.of(rawStats));

		Map<UUID, ChapterStats> statsByBookId = bookService.getChapterStats(List.of(bookWithChapters, bookWithoutChapters));

		assertThat(statsByBookId.get(bookWithChapters.getId())).isEqualTo(new ChapterStats(2, 300));
		assertThat(statsByBookId).doesNotContainKey(bookWithoutChapters.getId());
	}

	@Test
	void getChapterStatsReturnsEmptyMapWithoutQueryingForEmptyInput() {
		Map<UUID, ChapterStats> statsByBookId = bookService.getChapterStats(List.<Book>of());

		assertThat(statsByBookId).isEmpty();
	}

	private BookChapterStats bookChapterStats(UUID bookId, long chapterCount, long wordCount) {
		return new BookChapterStats() {
			@Override
			public UUID getBookId() {
				return bookId;
			}

			@Override
			public long getChapterCount() {
				return chapterCount;
			}

			@Override
			public long getWordCount() {
				return wordCount;
			}
		};
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
