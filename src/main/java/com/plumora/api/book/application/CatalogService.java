package com.plumora.api.book.application;

import com.plumora.api.book.domain.Book;
import com.plumora.api.book.domain.BookStatus;
import com.plumora.api.book.domain.BookVisibility;
import com.plumora.api.book.infrastructure.BookRepository;
import com.plumora.api.book.infrastructure.ChapterRepository;
import com.plumora.api.shared.exception.ResourceNotFoundException;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class CatalogService {

	private static final int DEFAULT_PAGE_SIZE = 20;
	private static final int MAX_PAGE_SIZE = 100;
	private static final BookStatus CATALOG_STATUS = BookStatus.PUBLISHED;
	private static final BookVisibility CATALOG_VISIBILITY = BookVisibility.PUBLIC;

	private final BookRepository bookRepository;
	private final ChapterRepository chapterRepository;

	public CatalogService(BookRepository bookRepository, ChapterRepository chapterRepository) {
		this.bookRepository = bookRepository;
		this.chapterRepository = chapterRepository;
	}

	@Transactional(readOnly = true)
	public Page<Book> getBooks(int page, int size) {
		return bookRepository.findCatalogBooks(
			CATALOG_STATUS,
			CATALOG_VISIBILITY,
			pageable(page, size, Sort.by(Sort.Direction.ASC, "title"))
		);
	}

	@Transactional(readOnly = true)
	public Book getBook(UUID bookId) {
		return bookRepository.findCatalogBookById(bookId, CATALOG_STATUS, CATALOG_VISIBILITY)
			.orElseThrow(() -> new ResourceNotFoundException("Catalog book was not found"));
	}

	@Transactional(readOnly = true)
	public Page<Book> searchBooks(String query, String genre, int page, int size) {
		return bookRepository.searchCatalogBooks(
			CATALOG_STATUS,
			CATALOG_VISIBILITY,
			normalize(query),
			normalize(genre),
			pageable(page, size, Sort.by(Sort.Direction.DESC, "publishedAt"))
		);
	}

	@Transactional(readOnly = true)
	public Page<Book> getPopularBooks(int page, int size) {
		return bookRepository.findCatalogBooks(
			CATALOG_STATUS,
			CATALOG_VISIBILITY,
			pageable(page, size, Sort.by(Sort.Direction.DESC, "readingCount")
				.and(Sort.by(Sort.Direction.DESC, "publishedAt")))
		);
	}

	@Transactional(readOnly = true)
	public Page<Book> getLatestBooks(int page, int size) {
		return bookRepository.findCatalogBooks(
			CATALOG_STATUS,
			CATALOG_VISIBILITY,
			pageable(page, size, Sort.by(Sort.Direction.DESC, "publishedAt"))
		);
	}

	@Transactional(readOnly = true)
	public List<String> getGenres() {
		return bookRepository.findCatalogGenres(CATALOG_STATUS, CATALOG_VISIBILITY);
	}

	@Transactional(readOnly = true)
	public long countChapters(Book book) {
		return chapterRepository.countByBook(book);
	}

	private Pageable pageable(int page, int size, Sort sort) {
		int safePage = Math.max(page, 0);
		int safeSize = size <= 0 ? DEFAULT_PAGE_SIZE : Math.min(size, MAX_PAGE_SIZE);
		return PageRequest.of(safePage, safeSize, sort);
	}

	private String normalize(String value) {
		return StringUtils.hasText(value) ? value.trim() : null;
	}
}
