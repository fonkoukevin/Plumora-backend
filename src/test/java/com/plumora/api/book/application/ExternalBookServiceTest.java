package com.plumora.api.book.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.plumora.api.book.domain.Book;
import com.plumora.api.book.domain.BookStatus;
import com.plumora.api.book.domain.BookVisibility;
import com.plumora.api.book.domain.Chapter;
import com.plumora.api.book.domain.ExternalBookSource;
import com.plumora.api.book.infrastructure.BookRepository;
import com.plumora.api.book.infrastructure.ChapterRepository;
import com.plumora.api.book.infrastructure.gutendex.GutendexAuthorResponse;
import com.plumora.api.book.infrastructure.gutendex.GutendexBookResponse;
import com.plumora.api.book.infrastructure.gutendex.GutendexClient;
import com.plumora.api.book.infrastructure.gutendex.GutendexContentClient;
import com.plumora.api.book.infrastructure.gutendex.GutendexPageResponse;
import com.plumora.api.book.infrastructure.gutendex.GutendexSearchRequest;
import com.plumora.api.book.infrastructure.openlibrary.OpenLibraryClient;
import com.plumora.api.book.infrastructure.openlibrary.OpenLibraryCoverService;
import com.plumora.api.book.infrastructure.openlibrary.OpenLibraryDocResponse;
import com.plumora.api.book.infrastructure.openlibrary.OpenLibrarySearchResponse;
import com.plumora.api.shared.exception.BusinessException;
import com.plumora.api.shared.exception.ExternalServiceUnavailableException;
import com.plumora.api.user.application.UserService;
import com.plumora.api.user.domain.User;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ExternalBookServiceTest {

	@Mock
	private GutendexClient gutendexClient;

	@Mock
	private GutendexContentClient gutendexContentClient;

	@Mock
	private ExternalBookContentSanitizer externalBookContentSanitizer;

	@Mock
	private OpenLibraryClient openLibraryClient;

	@Mock
	private OpenLibraryCoverService openLibraryCoverService;

	@Mock
	private BookRepository bookRepository;

	@Mock
	private ChapterRepository chapterRepository;

	@Mock
	private UserService userService;

	private ExternalBookService externalBookService;

	@BeforeEach
	void setUp() {
		externalBookService = new ExternalBookService(
			gutendexClient,
			gutendexContentClient,
			externalBookContentSanitizer,
			openLibraryClient,
			openLibraryCoverService,
			bookRepository,
			chapterRepository,
			userService
		);
	}

	@Test
	void searchExternalBooksMapsGutendexBooksAndPrefersGutendexCover() {
		GutendexBookResponse gutendexBook = gutendexBook(formats(
			"image/jpeg", "https://gutendex.test/cover.jpg",
			"text/plain; charset=utf-8", "https://gutendex.test/book.txt",
			"application/epub+zip", "https://gutendex.test/book.epub",
			"text/html; charset=utf-8", "https://gutendex.test/book.html"
		));
		when(gutendexClient.searchBooks(any(GutendexSearchRequest.class)))
			.thenReturn(new GutendexPageResponse(1, null, null, List.of(gutendexBook)));

		var page = externalBookService.searchExternalBooks(" Hugo ", " fr ", " fiction ", 0);

		ExternalBook book = page.getContent().getFirst();
		assertThat(book.externalId()).isEqualTo("123");
		assertThat(book.source()).isEqualTo("GUTENDEX");
		assertThat(book.authors()).containsExactly("Victor Hugo");
		assertThat(book.summary()).isEqualTo("Un roman social.");
		assertThat(book.coverUrl()).isEqualTo("https://gutendex.test/cover.jpg");
		assertThat(book.readUrl()).isEqualTo("https://gutendex.test/book.html");
		assertThat(book.sourceUrl()).isEqualTo("https://www.gutenberg.org/ebooks/123");
		verifyNoInteractions(openLibraryCoverService);

		ArgumentCaptor<GutendexSearchRequest> requestCaptor = ArgumentCaptor.forClass(GutendexSearchRequest.class);
		verify(gutendexClient).searchBooks(requestCaptor.capture());
		assertThat(requestCaptor.getValue().search()).isEqualTo("Hugo");
		assertThat(requestCaptor.getValue().language()).isEqualTo("fr");
		assertThat(requestCaptor.getValue().topic()).isEqualTo("fiction");
		assertThat(requestCaptor.getValue().page()).isEqualTo(1);
		assertThat(requestCaptor.getValue().sort()).isEqualTo("popular");
		assertThat(requestCaptor.getValue().copyright()).isFalse();
	}

	@Test
	void searchExternalBooksUsesOpenLibraryWhenGutendexHasNoImage() {
		GutendexBookResponse gutendexBook = gutendexBook(formats(
			"application/epub+zip", "https://gutendex.test/book.epub"
		));
		when(gutendexClient.searchBooks(any(GutendexSearchRequest.class)))
			.thenReturn(new GutendexPageResponse(1, null, null, List.of(gutendexBook)));
		when(openLibraryCoverService.resolveCoverUrl("Les Miserables", "Victor Hugo"))
			.thenReturn("https://covers.openlibrary.org/b/id/987-L.jpg?default=false");

		ExternalBook book = externalBookService.searchExternalBooks(null, null, null, 0)
			.getContent()
			.getFirst();

		assertThat(book.coverUrl()).isEqualTo("https://covers.openlibrary.org/b/id/987-L.jpg?default=false");
		assertThat(book.readUrl()).isEqualTo("https://gutendex.test/book.epub");
	}

	@Test
	void searchExternalBooksMarksAlreadyImportedBooks() {
		Book importedBook = importedBook();
		GutendexBookResponse gutendexBook = gutendexBook(formats(
			"text/plain; charset=utf-8", "https://gutendex.test/book.txt"
		));
		when(gutendexClient.searchBooks(any(GutendexSearchRequest.class)))
			.thenReturn(new GutendexPageResponse(1, null, null, List.of(gutendexBook)));
		when(bookRepository.findByExternalSourceAndExternalId(ExternalBookSource.GUTENDEX, "123"))
			.thenReturn(Optional.of(importedBook));
		when(openLibraryCoverService.resolveCoverUrl("Les Miserables", "Victor Hugo")).thenReturn(null);

		ExternalBook book = externalBookService.searchExternalBooks(null, null, null, 0)
			.getContent()
			.getFirst();

		assertThat(book.imported()).isTrue();
		assertThat(book.internalBookId()).isEqualTo(importedBook.getId());
	}

	@Test
	void searchExternalBooksFallsBackToOpenLibraryWhenGutendexIsUnavailable() {
		when(gutendexClient.searchBooks(any(GutendexSearchRequest.class)))
			.thenThrow(new ExternalServiceUnavailableException("Gutendex is currently unavailable"));
		OpenLibraryDocResponse doc = new OpenLibraryDocResponse(
			"/works/OL1W",
			"Les Miserables",
			List.of("Victor Hugo"),
			987,
			null,
			List.of("Fantasy")
		);
		when(openLibraryClient.searchBooks("Hugo", "fiction", 1))
			.thenReturn(new OpenLibrarySearchResponse(1, List.of(doc)));

		var page = externalBookService.searchExternalBooks(" Hugo ", "fr", " fiction ", 0);

		ExternalBook book = page.getContent().getFirst();
		assertThat(book.externalId()).isEqualTo("/works/OL1W");
		assertThat(book.source()).isEqualTo("OPEN_LIBRARY");
		assertThat(book.title()).isEqualTo("Les Miserables");
		assertThat(book.authors()).containsExactly("Victor Hugo");
		assertThat(book.subjects()).containsExactly("Fantasy");
		assertThat(book.coverUrl()).isEqualTo("https://covers.openlibrary.org/b/id/987-L.jpg?default=false");
		assertThat(book.readUrl()).isNull();
		assertThat(book.formats()).isEmpty();
		assertThat(book.imported()).isFalse();
		assertThat(book.internalBookId()).isNull();
		assertThat(book.sourceUrl()).isEqualTo("https://openlibrary.org/works/OL1W");
		verifyNoInteractions(openLibraryCoverService, bookRepository);
	}

	@Test
	void getDiscoverFiltersReturnsDefaultDiscoverTopics() {
		assertThat(externalBookService.getDiscoverFilters())
			.extracting(ExternalBookFilter::label)
			.containsExactly("Tous", "Fantasy", "Romance", "Thriller", "Sci-Fi", "Mystere", "Aventure", "Horreur");
		assertThat(externalBookService.getDiscoverFilters())
			.extracting(ExternalBookFilter::topic)
			.containsExactly(null, "fantasy", "romance", "thriller", "science fiction", "mystery", "adventure", "horror");
	}

	@Test
	void importGutendexBookReturnsExistingBookWhenAlreadyImported() {
		Book existing = importedBook();
		Chapter chapter = importedChapter(existing, "Already readable content");
		when(bookRepository.findByExternalSourceAndExternalId(ExternalBookSource.GUTENDEX, "123"))
			.thenReturn(Optional.of(existing));
		when(chapterRepository.findByBookOrderByChapterOrderAsc(existing)).thenReturn(List.of(chapter));

		ImportedExternalBookResult result = externalBookService.importGutendexBook("admin@example.com", 123);

		assertThat(result.created()).isFalse();
		assertThat(result.book()).isEqualTo(existing);
		verify(bookRepository, never()).save(any(Book.class));
		verifyNoInteractions(gutendexClient, gutendexContentClient, externalBookContentSanitizer, openLibraryCoverService, userService);
	}

	@Test
	void importGutendexBookRepairsExistingRedirectChapter() {
		Book existing = importedBook();
		Chapter chapter = importedChapter(existing, "302 Found\n\nFound\n\nThe document has moved here.");
		GutendexBookResponse gutendexBook = gutendexBook(formats(
			"text/plain; charset=utf-8", "https://gutendex.test/book.txt"
		));
		when(bookRepository.findByExternalSourceAndExternalId(ExternalBookSource.GUTENDEX, "123"))
			.thenReturn(Optional.of(existing));
		when(chapterRepository.findByBookOrderByChapterOrderAsc(existing)).thenReturn(List.of(chapter));
		when(gutendexClient.getBook(123)).thenReturn(Optional.of(gutendexBook));
		when(openLibraryCoverService.resolveCoverUrl("Les Miserables", "Victor Hugo")).thenReturn(null);
		when(gutendexContentClient.download("https://gutendex.test/book.txt")).thenReturn("RAW TEXT");
		when(externalBookContentSanitizer.sanitize("RAW TEXT", "text/plain; charset=utf-8"))
			.thenReturn("Clean text");

		ImportedExternalBookResult result = externalBookService.importGutendexBook("admin@example.com", 123);

		assertThat(result.created()).isFalse();
		assertThat(chapter.getTitle()).isEqualTo("Texte intégral");
		assertThat(chapter.getContent()).isEqualTo("Clean text");
		verify(chapterRepository).save(chapter);
		verify(bookRepository, never()).save(any(Book.class));
	}

	@Test
	void importGutendexBookPersistsPublishedPublicBookAndCreatesTextChapter() {
		User admin = user("admin@example.com");
		GutendexBookResponse gutendexBook = gutendexBook(formats(
			"text/plain; charset=utf-8", "https://gutendex.test/book.txt"
		));
		when(bookRepository.findByExternalSourceAndExternalId(ExternalBookSource.GUTENDEX, "123"))
			.thenReturn(Optional.empty());
		when(userService.getCurrentUser(admin.getEmail())).thenReturn(admin);
		when(gutendexClient.getBook(123)).thenReturn(Optional.of(gutendexBook));
		when(openLibraryCoverService.resolveCoverUrl("Les Miserables", "Victor Hugo"))
			.thenReturn("https://covers.openlibrary.org/b/id/987-L.jpg?default=false");
		when(gutendexContentClient.download("https://gutendex.test/book.txt")).thenReturn("RAW TEXT");
		when(externalBookContentSanitizer.sanitize("RAW TEXT", "text/plain; charset=utf-8"))
			.thenReturn("Clean text");
		when(bookRepository.save(any(Book.class))).thenAnswer(invocation -> invocation.getArgument(0));
		when(chapterRepository.save(any(Chapter.class))).thenAnswer(invocation -> invocation.getArgument(0));

		ImportedExternalBookResult result = externalBookService.importGutendexBook(admin.getEmail(), 123);

		assertThat(result.created()).isTrue();
		Book saved = result.book();
		assertThat(saved.getAuthor()).isEqualTo(admin);
		assertThat(saved.getStatus()).isEqualTo(BookStatus.PUBLISHED);
		assertThat(saved.getVisibility()).isEqualTo(BookVisibility.PUBLIC);
		assertThat(saved.getPublishedAt()).isNotNull();
		assertThat(saved.getExternalSource()).isEqualTo(ExternalBookSource.GUTENDEX);
		assertThat(saved.getExternalId()).isEqualTo("123");
		assertThat(saved.getExternalAuthors()).containsExactly("Victor Hugo");
		assertThat(saved.getReadUrl()).isEqualTo("https://gutendex.test/book.txt");
		assertThat(saved.getCoverUrl()).isEqualTo("https://covers.openlibrary.org/b/id/987-L.jpg?default=false");
		ArgumentCaptor<Chapter> chapterCaptor = ArgumentCaptor.forClass(Chapter.class);
		verify(chapterRepository).save(chapterCaptor.capture());
		Chapter chapter = chapterCaptor.getValue();
		assertThat(chapter.getBook()).isEqualTo(saved);
		assertThat(chapter.getTitle()).isEqualTo("Texte intégral");
		assertThat(chapter.getContent()).isEqualTo("Clean text");
		assertThat(chapter.getChapterOrder()).isEqualTo(1);
	}

	@Test
	void importGutendexBookPrefersHtmlContentForFullTextChapter() {
		User admin = user("admin@example.com");
		GutendexBookResponse gutendexBook = gutendexBook(formats(
			"text/plain; charset=utf-8", "https://gutendex.test/book.txt",
			"text/html; charset=utf-8", "https://gutendex.test/book.html"
		));
		when(bookRepository.findByExternalSourceAndExternalId(ExternalBookSource.GUTENDEX, "123"))
			.thenReturn(Optional.empty());
		when(userService.getCurrentUser(admin.getEmail())).thenReturn(admin);
		when(gutendexClient.getBook(123)).thenReturn(Optional.of(gutendexBook));
		when(openLibraryCoverService.resolveCoverUrl("Les Miserables", "Victor Hugo")).thenReturn(null);
		when(gutendexContentClient.download("https://gutendex.test/book.html")).thenReturn("<p>RAW HTML</p>");
		when(externalBookContentSanitizer.sanitize("<p>RAW HTML</p>", "text/html; charset=utf-8"))
			.thenReturn("Raw HTML");
		when(bookRepository.save(any(Book.class))).thenAnswer(invocation -> invocation.getArgument(0));

		externalBookService.importGutendexBook(admin.getEmail(), 123);

		verify(gutendexContentClient).download("https://gutendex.test/book.html");
	}

	@Test
	void importGutendexBookRejectsBookWithoutReadableContentFormat() {
		User admin = user("admin@example.com");
		GutendexBookResponse gutendexBook = gutendexBook(formats(
			"application/epub+zip", "https://gutendex.test/book.epub"
		));
		when(bookRepository.findByExternalSourceAndExternalId(ExternalBookSource.GUTENDEX, "123"))
			.thenReturn(Optional.empty());
		when(userService.getCurrentUser(admin.getEmail())).thenReturn(admin);
		when(gutendexClient.getBook(123)).thenReturn(Optional.of(gutendexBook));
		when(openLibraryCoverService.resolveCoverUrl("Les Miserables", "Victor Hugo")).thenReturn(null);

		assertThatThrownBy(() -> externalBookService.importGutendexBook(admin.getEmail(), 123))
			.isInstanceOf(BusinessException.class)
			.hasMessage("No readable Gutendex content format is available");
		verify(bookRepository, never()).save(any(Book.class));
		verifyNoInteractions(gutendexContentClient, externalBookContentSanitizer);
	}

	private GutendexBookResponse gutendexBook(Map<String, String> formats) {
		return new GutendexBookResponse(
			123,
			"Les Miserables",
			List.of(new GutendexAuthorResponse("Victor Hugo", 1802, 1885)),
			List.of("Un roman social."),
			List.of("French fiction"),
			List.of("FR Litterature"),
			List.of("fr"),
			false,
			"Text",
			formats,
			42
		);
	}

	private Map<String, String> formats(String... values) {
		Map<String, String> formats = new LinkedHashMap<>();
		for (int index = 0; index < values.length; index += 2) {
			formats.put(values[index], values[index + 1]);
		}
		return formats;
	}

	private Book importedBook() {
		Book book = new Book();
		book.setId(UUID.randomUUID());
		book.setAuthor(user("admin@example.com"));
		book.setTitle("Les Miserables");
		book.setGenre("French fiction");
		book.setStatus(BookStatus.PUBLISHED);
		book.setVisibility(BookVisibility.PUBLIC);
		book.setExternalSource(ExternalBookSource.GUTENDEX);
		book.setExternalId("123");
		return book;
	}

	private Chapter importedChapter(Book book, String content) {
		Chapter chapter = new Chapter();
		chapter.setId(UUID.randomUUID());
		chapter.setBook(book);
		chapter.setTitle("Texte intégral");
		chapter.setChapterOrder(1);
		chapter.setContent(content);
		return chapter;
	}

	private User user(String email) {
		User user = new User();
		user.setId(UUID.randomUUID());
		user.setFirstname("Test");
		user.setLastname("User");
		user.setEmail(email);
		user.setUsername(email.substring(0, email.indexOf('@')));
		return user;
	}
}
