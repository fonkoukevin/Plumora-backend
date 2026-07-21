package com.plumora.api.book.application;

import com.plumora.api.book.domain.Book;
import com.plumora.api.book.domain.BookStatus;
import com.plumora.api.book.domain.BookVisibility;
import com.plumora.api.book.domain.Chapter;
import com.plumora.api.book.domain.ExternalBookSource;
import com.plumora.api.book.infrastructure.BookRepository;
import com.plumora.api.book.infrastructure.ChapterRepository;
import com.plumora.api.book.infrastructure.gutendex.GutendexContentClient;
import com.plumora.api.book.infrastructure.gutendex.GutendexAuthorResponse;
import com.plumora.api.book.infrastructure.gutendex.GutendexBookResponse;
import com.plumora.api.book.infrastructure.gutendex.GutendexClient;
import com.plumora.api.book.infrastructure.gutendex.GutendexPageResponse;
import com.plumora.api.book.infrastructure.gutendex.GutendexSearchRequest;
import com.plumora.api.book.infrastructure.openlibrary.OpenLibraryClient;
import com.plumora.api.book.infrastructure.openlibrary.OpenLibraryCoverService;
import com.plumora.api.book.infrastructure.openlibrary.OpenLibraryDocResponse;
import com.plumora.api.book.infrastructure.openlibrary.OpenLibrarySearchResponse;
import com.plumora.api.shared.exception.BusinessException;
import com.plumora.api.shared.exception.ExternalServiceUnavailableException;
import com.plumora.api.shared.exception.ResourceNotFoundException;
import com.plumora.api.user.application.UserService;
import com.plumora.api.user.domain.User;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class ExternalBookService {

	private static final int GUTENDEX_PAGE_SIZE = 32;
	private static final String GUTENBERG_SOURCE_URL = "https://www.gutenberg.org/ebooks/";
	private static final String DEFAULT_EXTERNAL_GENRE = "Public Domain";
	private static final List<ExternalBookFilter> DISCOVER_FILTERS = List.of(
		new ExternalBookFilter("Tous", null),
		new ExternalBookFilter("Fantasy", "fantasy"),
		new ExternalBookFilter("Romance", "romance"),
		new ExternalBookFilter("Thriller", "thriller"),
		new ExternalBookFilter("Sci-Fi", "science fiction"),
		new ExternalBookFilter("Mystere", "mystery"),
		new ExternalBookFilter("Aventure", "adventure"),
		new ExternalBookFilter("Horreur", "horror")
	);

	private final GutendexClient gutendexClient;
	private final GutendexContentClient gutendexContentClient;
	private final ExternalBookContentSanitizer externalBookContentSanitizer;
	private final OpenLibraryClient openLibraryClient;
	private final OpenLibraryCoverService openLibraryCoverService;
	private final BookRepository bookRepository;
	private final ChapterRepository chapterRepository;
	private final UserService userService;

	public ExternalBookService(
		GutendexClient gutendexClient,
		GutendexContentClient gutendexContentClient,
		ExternalBookContentSanitizer externalBookContentSanitizer,
		OpenLibraryClient openLibraryClient,
		OpenLibraryCoverService openLibraryCoverService,
		BookRepository bookRepository,
		ChapterRepository chapterRepository,
		UserService userService
	) {
		this.gutendexClient = gutendexClient;
		this.gutendexContentClient = gutendexContentClient;
		this.externalBookContentSanitizer = externalBookContentSanitizer;
		this.openLibraryClient = openLibraryClient;
		this.openLibraryCoverService = openLibraryCoverService;
		this.bookRepository = bookRepository;
		this.chapterRepository = chapterRepository;
		this.userService = userService;
	}

	@Transactional(readOnly = true)
	public Page<ExternalBook> searchExternalBooks(String search, String language, String topic, int page) {
		int safePage = Math.max(page, 0);
		try {
			GutendexSearchRequest request = GutendexSearchRequest.publicDomain(
				normalize(search),
				normalize(language),
				normalize(topic),
				safePage + 1
			);
			GutendexPageResponse response = gutendexClient.searchBooks(request);
			List<ExternalBook> books = safeResults(response).stream()
				.map(this::toExternalBook)
				.toList();
			long total = Math.max(response.count(), books.size());
			return new PageImpl<>(books, PageRequest.of(safePage, GUTENDEX_PAGE_SIZE), total);
		} catch (ExternalServiceUnavailableException exception) {
			// Gutendex (gutendex.com) is unreachable - fall back to Open Library for discovery.
			// Open Library is a metadata catalog, not a full-text source: results mapped from it
			// never carry a readUrl/formats, so the app must not offer to import/read them, only
			// browse (see toExternalBook(OpenLibraryDocResponse)).
			return searchOpenLibraryBooks(normalize(search), normalize(topic), safePage);
		}
	}

	private Page<ExternalBook> searchOpenLibraryBooks(String search, String topic, int safePage) {
		OpenLibrarySearchResponse response = openLibraryClient.searchBooks(search, topic, safePage + 1);
		List<ExternalBook> books = safeDocs(response).stream()
			.filter(doc -> StringUtils.hasText(doc.title()))
			.map(this::toExternalBook)
			.toList();
		long total = Math.max(response.numFound(), books.size());
		return new PageImpl<>(books, PageRequest.of(safePage, GUTENDEX_PAGE_SIZE), total);
	}

	public List<ExternalBookFilter> getDiscoverFilters() {
		return DISCOVER_FILTERS;
	}

	@Transactional(readOnly = true)
	public ExternalBook getExternalBook(int gutendexId) {
		return gutendexClient.getBook(gutendexId)
			.map(this::toExternalBook)
			.orElseThrow(() -> new ResourceNotFoundException("External book was not found"));
	}

	@Transactional
	public ImportedExternalBookResult importGutendexBook(String currentUserEmail, int gutendexId) {
		String externalId = String.valueOf(gutendexId);
		Optional<Book> existingBook = bookRepository.findByExternalSourceAndExternalId(ExternalBookSource.GUTENDEX, externalId);
		if (existingBook.isPresent()) {
			ensureReadableChapter(existingBook.get(), gutendexId);
			return new ImportedExternalBookResult(existingBook.get(), false);
		}

		User importer = userService.getCurrentUser(currentUserEmail);
		ExternalBook externalBook = getExternalBook(gutendexId);
		String content = downloadReadableContent(externalBook);

		Book book = new Book();
		book.setAuthor(importer);
		book.setTitle(limit(StringUtils.hasText(externalBook.title()) ? externalBook.title() : "Gutendex book " + gutendexId, 150));
		book.setSummary(externalBook.summary());
		book.setCoverUrl(limit(externalBook.coverUrl(), 500));
		book.setGenre(limit(chooseGenre(externalBook.subjects()), 80));
		book.setLanguageCode(firstOrDefault(externalBook.languages(), "fr"));
		book.setStatus(BookStatus.PUBLISHED);
		book.setVisibility(BookVisibility.PUBLIC);
		book.setPublishedAt(LocalDateTime.now());
		book.setExternalSource(ExternalBookSource.GUTENDEX);
		book.setExternalId(externalId);
		book.setExternalAuthors(externalBook.authors());
		book.setExternalSubjects(externalBook.subjects());
		book.setExternalLanguages(externalBook.languages());
		book.setSourceUrl(limit(externalBook.sourceUrl(), 500));
		book.setReadUrl(limit(externalBook.readUrl(), 1000));
		book.setDownloadCount(externalBook.downloadCount());
		book.setFormatsJson(externalBook.formats());

		try {
			Book savedBook = bookRepository.save(book);
			createFullTextChapter(savedBook, content);
			return new ImportedExternalBookResult(savedBook, true);
		} catch (DataIntegrityViolationException exception) {
			return bookRepository.findByExternalSourceAndExternalId(ExternalBookSource.GUTENDEX, externalId)
				.map(existing -> {
					ensureReadableChapter(existing, gutendexId);
					return new ImportedExternalBookResult(existing, false);
				})
				.orElseThrow(() -> exception);
		}
	}

	private ExternalBook toExternalBook(GutendexBookResponse book) {
		Map<String, String> formats = safeFormats(book.formats());
		List<String> authors = authorNames(book.authors());
		String coverUrl = coverFromFormats(formats);
		if (!StringUtils.hasText(coverUrl)) {
			coverUrl = openLibraryCoverService.resolveCoverUrl(book.title(), authors.isEmpty() ? null : authors.getFirst());
		}
		Optional<java.util.UUID> importedBookId = importedBookId(book.id());

		return new ExternalBook(
			String.valueOf(book.id()),
			ExternalBookSource.GUTENDEX.name(),
			book.title(),
			authors,
			firstNonBlank(book.summaries()),
			safeList(book.subjects()),
			safeList(book.languages()),
			book.copyright(),
			book.mediaType(),
			book.downloadCount(),
			coverUrl,
			readUrl(formats),
			formats,
			GUTENBERG_SOURCE_URL + book.id(),
			importedBookId.isPresent(),
			importedBookId.orElse(null)
		);
	}

	private ExternalBook toExternalBook(OpenLibraryDocResponse doc) {
		return new ExternalBook(
			doc.key(),
			"OPEN_LIBRARY",
			doc.title(),
			safeList(doc.authorName()),
			null,
			safeList(doc.subject()),
			List.of(),
			null,
			null,
			0,
			openLibraryCoverUrl(doc),
			null,
			Map.of(),
			StringUtils.hasText(doc.key()) ? "https://openlibrary.org" + doc.key() : null,
			false,
			null
		);
	}

	private String openLibraryCoverUrl(OpenLibraryDocResponse doc) {
		if (doc.coverId() != null) {
			return "https://covers.openlibrary.org/b/id/" + doc.coverId() + "-L.jpg?default=false";
		}
		if (doc.isbn() != null) {
			return doc.isbn().stream()
				.filter(StringUtils::hasText)
				.findFirst()
				.map(isbn -> "https://covers.openlibrary.org/b/isbn/" + isbn + "-L.jpg?default=false")
				.orElse(null);
		}
		return null;
	}

	private List<OpenLibraryDocResponse> safeDocs(OpenLibrarySearchResponse response) {
		if (response == null || response.docs() == null) {
			return List.of();
		}
		return response.docs();
	}

	private void ensureReadableChapter(Book book, int gutendexId) {
		List<Chapter> chapters = chapterRepository.findByBookOrderByChapterOrderAsc(book);
		if (!chapters.isEmpty() && !looksLikeRedirectArtifact(chapters.getFirst().getContent())) {
			return;
		}
		ExternalBook externalBook = getExternalBook(gutendexId);
		String content = downloadReadableContent(externalBook);
		if (chapters.isEmpty()) {
			createFullTextChapter(book, content);
			return;
		}
		Chapter chapter = chapters.getFirst();
		chapter.setTitle("Texte intégral");
		chapter.setContent(content);
		chapter.setChapterOrder(1);
		chapter.updateWordCount();
		chapterRepository.save(chapter);
	}

	private String downloadReadableContent(ExternalBook externalBook) {
		ReadableExternalBookContent readableContent = readableContent(externalBook.formats())
			.orElseThrow(() -> new BusinessException("No readable Gutendex content format is available"));
		String rawContent = gutendexContentClient.download(readableContent.url());
		String content = externalBookContentSanitizer.sanitize(rawContent, readableContent.mediaType());
		if (!StringUtils.hasText(content)) {
			throw new BusinessException("Downloaded Gutendex content is empty");
		}
		if (looksLikeRedirectArtifact(content)) {
			throw new ExternalServiceUnavailableException("Gutendex content is currently unavailable");
		}
		return content;
	}

	private boolean looksLikeRedirectArtifact(String content) {
		if (!StringUtils.hasText(content)) {
			return false;
		}
		String normalizedContent = content.stripLeading().toLowerCase(Locale.ROOT);
		return normalizedContent.startsWith("301 moved")
			|| normalizedContent.startsWith("302 found")
			|| normalizedContent.startsWith("307 temporary redirect")
			|| normalizedContent.startsWith("308 permanent redirect");
	}

	private void createFullTextChapter(Book book, String content) {
		Chapter chapter = new Chapter();
		chapter.setBook(book);
		chapter.setTitle("Texte intégral");
		chapter.setContent(content);
		chapter.setChapterOrder(1);
		chapter.updateWordCount();
		chapterRepository.save(chapter);
	}

	private Optional<ReadableExternalBookContent> readableContent(Map<String, String> formats) {
		for (String mediaType : List.of("text/html", "text/plain")) {
			Optional<ReadableExternalBookContent> content = readableContent(formats, mediaType);
			if (content.isPresent()) {
				return content;
			}
		}
		return Optional.empty();
	}

	private Optional<ReadableExternalBookContent> readableContent(Map<String, String> formats, String mediaType) {
		return formats.entrySet()
			.stream()
			.filter(entry -> entry.getKey() != null && entry.getKey().toLowerCase(Locale.ROOT).startsWith(mediaType))
			.filter(entry -> StringUtils.hasText(entry.getValue()))
			.findFirst()
			.map(entry -> new ReadableExternalBookContent(entry.getKey(), entry.getValue()));
	}

	private Optional<java.util.UUID> importedBookId(int gutendexId) {
		Optional<Book> importedBook = bookRepository.findByExternalSourceAndExternalId(
			ExternalBookSource.GUTENDEX,
			String.valueOf(gutendexId)
		);
		if (importedBook == null) {
			return Optional.empty();
		}
		return importedBook.map(Book::getId);
	}

	private List<GutendexBookResponse> safeResults(GutendexPageResponse response) {
		if (response == null || response.results() == null) {
			return List.of();
		}
		return response.results();
	}

	private List<String> authorNames(List<GutendexAuthorResponse> authors) {
		if (authors == null) {
			return List.of();
		}
		return authors.stream()
			.map(GutendexAuthorResponse::name)
			.filter(StringUtils::hasText)
			.map(String::trim)
			.toList();
	}

	private String coverFromFormats(Map<String, String> formats) {
		String jpegCover = formatUrl(formats, "image/jpeg");
		if (StringUtils.hasText(jpegCover)) {
			return jpegCover;
		}
		return formats.entrySet()
			.stream()
			.filter(entry -> entry.getKey() != null && entry.getKey().toLowerCase(Locale.ROOT).startsWith("image/"))
			.map(Map.Entry::getValue)
			.filter(StringUtils::hasText)
			.findFirst()
			.orElse(null);
	}

	private String readUrl(Map<String, String> formats) {
		for (String mediaType : List.of("text/html", "application/epub+zip", "text/plain")) {
			String url = formatUrl(formats, mediaType);
			if (StringUtils.hasText(url)) {
				return url;
			}
		}
		return null;
	}

	private String formatUrl(Map<String, String> formats, String mediaType) {
		return formats.entrySet()
			.stream()
			.filter(entry -> entry.getKey() != null && entry.getKey().toLowerCase(Locale.ROOT).startsWith(mediaType))
			.map(Map.Entry::getValue)
			.filter(StringUtils::hasText)
			.findFirst()
			.orElse(null);
	}

	private Map<String, String> safeFormats(Map<String, String> formats) {
		if (formats == null || formats.isEmpty()) {
			return Map.of();
		}
		return Collections.unmodifiableMap(new LinkedHashMap<>(formats));
	}

	private List<String> safeList(List<String> values) {
		if (values == null) {
			return List.of();
		}
		return values.stream()
			.filter(StringUtils::hasText)
			.map(String::trim)
			.toList();
	}

	private String firstNonBlank(List<String> values) {
		if (values == null) {
			return null;
		}
		return values.stream()
			.filter(StringUtils::hasText)
			.map(String::trim)
			.findFirst()
			.orElse(null);
	}

	private String chooseGenre(List<String> subjects) {
		return subjects.stream()
			.filter(StringUtils::hasText)
			.findFirst()
			.orElse(DEFAULT_EXTERNAL_GENRE);
	}

	private String firstOrDefault(List<String> values, String fallback) {
		return values.stream()
			.filter(StringUtils::hasText)
			.findFirst()
			.orElse(fallback);
	}

	private String normalize(String value) {
		return StringUtils.hasText(value) ? value.trim() : null;
	}

	private String limit(String value, int maxLength) {
		if (value == null || value.length() <= maxLength) {
			return value;
		}
		return value.substring(0, maxLength);
	}
}
