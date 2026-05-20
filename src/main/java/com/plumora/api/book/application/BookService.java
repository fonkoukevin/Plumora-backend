package com.plumora.api.book.application;

import com.plumora.api.book.domain.Book;
import com.plumora.api.book.domain.BookStatus;
import com.plumora.api.book.domain.BookVisibility;
import com.plumora.api.book.infrastructure.BookRepository;
import com.plumora.api.book.infrastructure.ChapterRepository;
import com.plumora.api.book.presentation.CreateBookRequest;
import com.plumora.api.book.presentation.UpdateBookRequest;
import com.plumora.api.shared.exception.BusinessException;
import com.plumora.api.shared.exception.ResourceNotFoundException;
import com.plumora.api.shared.exception.UnauthorizedActionException;
import com.plumora.api.user.application.UserService;
import com.plumora.api.user.domain.User;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class BookService {

	private final BookRepository bookRepository;
	private final ChapterRepository chapterRepository;
	private final UserService userService;

	public BookService(
		BookRepository bookRepository,
		ChapterRepository chapterRepository,
		UserService userService
	) {
		this.bookRepository = bookRepository;
		this.chapterRepository = chapterRepository;
		this.userService = userService;
	}

	@Transactional
	public Book createBook(String currentUserEmail, CreateBookRequest request) {
		User author = userService.getCurrentUser(currentUserEmail);
		Book book = new Book();
		book.setAuthor(author);
		applyBookFields(book, request.title(), request.subtitle(), request.summary(), request.coverUrl(), request.genre(), request.languageCode());
		book.setStatus(BookStatus.DRAFT);
		book.setVisibility(BookVisibility.PRIVATE);
		return bookRepository.save(book);
	}

	@Transactional(readOnly = true)
	public List<Book> getMyBooks(String currentUserEmail) {
		User author = userService.getCurrentUser(currentUserEmail);
		return bookRepository.findByAuthorOrderByCreatedAtDesc(author);
	}

	@Transactional(readOnly = true)
	public Book getBook(UUID bookId) {
		return findBook(bookId);
	}

	@Transactional
	public Book updateBook(String currentUserEmail, UUID bookId, UpdateBookRequest request) {
		Book book = getOwnedEditableBook(currentUserEmail, bookId);
		applyBookFields(book, request.title(), request.subtitle(), request.summary(), request.coverUrl(), request.genre(), request.languageCode());
		return bookRepository.save(book);
	}

	@Transactional
	public void deleteBook(String currentUserEmail, UUID bookId) {
		archiveBook(currentUserEmail, bookId);
	}

	@Transactional
	public Book markReady(String currentUserEmail, UUID bookId) {
		Book book = getOwnedEditableBook(currentUserEmail, bookId);
		book.setStatus(BookStatus.READY_TO_PUBLISH);
		return bookRepository.save(book);
	}

	@Transactional
	public Book publishBook(String currentUserEmail, UUID bookId) {
		Book book = getOwnedEditableBook(currentUserEmail, bookId);
		if (chapterRepository.countByBook(book) == 0) {
			throw new BusinessException("A book must have at least one chapter before publication");
		}
		book.setStatus(BookStatus.PUBLISHED);
		book.setVisibility(BookVisibility.PUBLIC);
		book.setPublishedAt(LocalDateTime.now());
		return bookRepository.save(book);
	}

	@Transactional
	public Book archiveBook(String currentUserEmail, UUID bookId) {
		Book book = getOwnedBook(currentUserEmail, bookId);
		book.setStatus(BookStatus.ARCHIVED);
		return bookRepository.save(book);
	}

	public Book getOwnedBook(String currentUserEmail, UUID bookId) {
		Book book = findBook(bookId);
		if (!book.getAuthor().getEmail().equals(currentUserEmail)) {
			throw new UnauthorizedActionException("Only the author can manage this book");
		}
		return book;
	}

	public Book getOwnedEditableBook(String currentUserEmail, UUID bookId) {
		Book book = getOwnedBook(currentUserEmail, bookId);
		ensureEditable(book);
		return book;
	}

	public void ensureEditable(Book book) {
		if (book.getStatus() == BookStatus.ARCHIVED) {
			throw new BusinessException("Archived books cannot be edited");
		}
	}

	private Book findBook(UUID bookId) {
		return bookRepository.findById(bookId)
			.orElseThrow(() -> new ResourceNotFoundException("Book was not found"));
	}

	private void applyBookFields(
		Book book,
		String title,
		String subtitle,
		String summary,
		String coverUrl,
		String genre,
		String languageCode
	) {
		book.setTitle(title);
		book.setSubtitle(subtitle);
		book.setSummary(summary);
		book.setCoverUrl(coverUrl);
		book.setGenre(genre);
		book.setLanguageCode(StringUtils.hasText(languageCode) ? languageCode : "fr");
	}
}
