package com.plumora.api.reading.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.plumora.api.book.domain.Book;
import com.plumora.api.book.domain.BookStatus;
import com.plumora.api.book.domain.BookVisibility;
import com.plumora.api.book.domain.Chapter;
import com.plumora.api.book.infrastructure.BookRepository;
import com.plumora.api.book.infrastructure.ChapterRepository;
import com.plumora.api.reading.domain.ReadingProgress;
import com.plumora.api.reading.infrastructure.ReadingProgressRepository;
import com.plumora.api.reading.presentation.ReadingProgressRequest;
import com.plumora.api.shared.exception.BusinessException;
import com.plumora.api.shared.exception.DuplicateResourceException;
import com.plumora.api.user.application.UserService;
import com.plumora.api.user.domain.User;
import java.math.BigDecimal;
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
class ReadingServiceTest {

	@Mock
	private BookRepository bookRepository;

	@Mock
	private ChapterRepository chapterRepository;

	@Mock
	private ReadingProgressRepository readingProgressRepository;

	@Mock
	private UserService userService;

	private ReadingService readingService;

	@BeforeEach
	void setUp() {
		readingService = new ReadingService(bookRepository, chapterRepository, readingProgressRepository, userService);
	}

	@Test
	void readBookCreatesProgressAndIncrementsReadingCountOnce() {
		User reader = user("reader@example.com");
		Book book = publishedBook(user("author@example.com"));
		Chapter chapter = chapter(book);

		when(userService.getCurrentUser(reader.getEmail())).thenReturn(reader);
		when(bookRepository.findByIdWithAuthor(book.getId())).thenReturn(Optional.of(book));
		when(chapterRepository.findByBookOrderByChapterOrderAsc(book)).thenReturn(List.of(chapter));
		when(readingProgressRepository.findByUserAndBook(reader, book)).thenReturn(Optional.empty());
		when(readingProgressRepository.save(any(ReadingProgress.class))).thenAnswer(invocation -> {
			ReadingProgress progress = invocation.getArgument(0);
			progress.setId(UUID.randomUUID());
			return progress;
		});

		ReadSession session = readingService.readBook(reader.getEmail(), book.getId());

		assertThat(session.progress().getBook()).isEqualTo(book);
		assertThat(session.progress().getCurrentChapter()).isEqualTo(chapter);
		assertThat(session.progress().getProgressPercentage()).isEqualByComparingTo("0.00");
		assertThat(book.getReadingCount()).isEqualTo(1);
		verify(bookRepository).save(book);
	}

	@Test
	void createProgressRejectsDuplicateForSameReaderAndBook() {
		User reader = user("reader@example.com");
		Book book = publishedBook(user("author@example.com"));

		when(userService.getCurrentUser(reader.getEmail())).thenReturn(reader);
		when(bookRepository.findByIdWithAuthor(book.getId())).thenReturn(Optional.of(book));
		when(readingProgressRepository.existsByUserAndBook(reader, book)).thenReturn(true);

		ReadingProgressRequest request = new ReadingProgressRequest(null, BigDecimal.TEN);

		assertThatThrownBy(() -> readingService.createProgress(reader.getEmail(), book.getId(), request))
			.isInstanceOf(DuplicateResourceException.class)
			.hasMessage("Reading progress already exists for this book");
	}

	@Test
	void updateProgressRejectsPercentageOutsideBounds() {
		User reader = user("reader@example.com");
		Book book = publishedBook(user("author@example.com"));
		ReadingProgress progress = progress(reader, book);

		when(userService.getCurrentUser(reader.getEmail())).thenReturn(reader);
		when(bookRepository.findByIdWithAuthor(book.getId())).thenReturn(Optional.of(book));
		when(readingProgressRepository.findByUserAndBook(reader, book)).thenReturn(Optional.of(progress));

		ReadingProgressRequest request = new ReadingProgressRequest(null, new BigDecimal("101.00"));

		assertThatThrownBy(() -> readingService.updateProgress(reader.getEmail(), book.getId(), request))
			.isInstanceOf(BusinessException.class)
			.hasMessage("Progress percentage must be between 0 and 100");
	}

	@Test
	void readingRequiresPublishedPublicBook() {
		User reader = user("reader@example.com");
		Book book = publishedBook(user("author@example.com"));
		book.setVisibility(BookVisibility.PRIVATE);

		when(userService.getCurrentUser(reader.getEmail())).thenReturn(reader);
		when(bookRepository.findByIdWithAuthor(book.getId())).thenReturn(Optional.of(book));

		assertThatThrownBy(() -> readingService.readBook(reader.getEmail(), book.getId()))
			.isInstanceOf(BusinessException.class)
			.hasMessage("Only published public books can be read");
	}

	private ReadingProgress progress(User user, Book book) {
		ReadingProgress progress = new ReadingProgress();
		progress.setId(UUID.randomUUID());
		progress.setUser(user);
		progress.setBook(book);
		progress.setStartedAt(LocalDateTime.now());
		progress.setProgressPercentage(BigDecimal.ZERO);
		return progress;
	}

	private Chapter chapter(Book book) {
		Chapter chapter = new Chapter();
		chapter.setId(UUID.randomUUID());
		chapter.setBook(book);
		chapter.setTitle("Chapter 1");
		chapter.setChapterOrder(1);
		chapter.setContent("Once upon a page");
		return chapter;
	}

	private Book publishedBook(User author) {
		Book book = new Book();
		book.setId(UUID.randomUUID());
		book.setAuthor(author);
		book.setTitle("Published book");
		book.setGenre("Fantasy");
		book.setStatus(BookStatus.PUBLISHED);
		book.setVisibility(BookVisibility.PUBLIC);
		book.setPublishedAt(LocalDateTime.now());
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
