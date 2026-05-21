package com.plumora.api.reading.application;

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
import com.plumora.api.shared.exception.ResourceNotFoundException;
import com.plumora.api.user.application.UserService;
import com.plumora.api.user.domain.User;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReadingService {

	private static final BigDecimal MIN_PROGRESS = new BigDecimal("0.00");
	private static final BigDecimal MAX_PROGRESS = new BigDecimal("100.00");

	private final BookRepository bookRepository;
	private final ChapterRepository chapterRepository;
	private final ReadingProgressRepository readingProgressRepository;
	private final UserService userService;

	public ReadingService(
		BookRepository bookRepository,
		ChapterRepository chapterRepository,
		ReadingProgressRepository readingProgressRepository,
		UserService userService
	) {
		this.bookRepository = bookRepository;
		this.chapterRepository = chapterRepository;
		this.readingProgressRepository = readingProgressRepository;
		this.userService = userService;
	}

	@Transactional
	public ReadSession readBook(String currentUserEmail, UUID bookId) {
		User user = userService.getCurrentUser(currentUserEmail);
		Book book = getPublishedPublicBook(bookId);
		List<Chapter> chapters = chapterRepository.findByBookOrderByChapterOrderAsc(book);
		ReadingProgress progress = readingProgressRepository.findByUserAndBook(user, book)
			.orElseGet(() -> createInitialProgress(user, book, chapters));
		return new ReadSession(book, chapters, progress);
	}

	@Transactional(readOnly = true)
	public List<ReadingProgress> getMyProgress(String currentUserEmail) {
		User user = userService.getCurrentUser(currentUserEmail);
		return readingProgressRepository.findByUserOrderByLastReadAtDescStartedAtDesc(user);
	}

	@Transactional(readOnly = true)
	public ReadingProgress getBookProgress(String currentUserEmail, UUID bookId) {
		User user = userService.getCurrentUser(currentUserEmail);
		Book book = getPublishedPublicBook(bookId);
		return readingProgressRepository.findByUserAndBook(user, book)
			.orElseThrow(() -> new ResourceNotFoundException("Reading progress was not found"));
	}

	@Transactional
	public ReadingProgress createProgress(String currentUserEmail, UUID bookId, ReadingProgressRequest request) {
		User user = userService.getCurrentUser(currentUserEmail);
		Book book = getPublishedPublicBook(bookId);
		if (readingProgressRepository.existsByUserAndBook(user, book)) {
			throw new DuplicateResourceException("Reading progress already exists for this book");
		}

		ReadingProgress progress = new ReadingProgress();
		progress.setUser(user);
		progress.setBook(book);
		progress.setStartedAt(LocalDateTime.now());
		applyProgress(progress, book, request);
		incrementReadingCount(book);
		return readingProgressRepository.save(progress);
	}

	@Transactional
	public ReadingProgress updateProgress(String currentUserEmail, UUID bookId, ReadingProgressRequest request) {
		User user = userService.getCurrentUser(currentUserEmail);
		Book book = getPublishedPublicBook(bookId);
		ReadingProgress progress = readingProgressRepository.findByUserAndBook(user, book)
			.orElseThrow(() -> new ResourceNotFoundException("Reading progress was not found"));
		applyProgress(progress, book, request);
		return readingProgressRepository.save(progress);
	}

	@Transactional
	public ReadingProgress finishProgress(String currentUserEmail, UUID bookId) {
		User user = userService.getCurrentUser(currentUserEmail);
		Book book = getPublishedPublicBook(bookId);
		ReadingProgress progress = readingProgressRepository.findByUserAndBook(user, book)
			.orElseThrow(() -> new ResourceNotFoundException("Reading progress was not found"));
		LocalDateTime now = LocalDateTime.now();
		progress.setProgressPercentage(MAX_PROGRESS);
		progress.setLastReadAt(now);
		progress.setFinishedAt(now);
		return readingProgressRepository.save(progress);
	}

	private ReadingProgress createInitialProgress(User user, Book book, List<Chapter> chapters) {
		ReadingProgress progress = new ReadingProgress();
		progress.setUser(user);
		progress.setBook(book);
		if (!chapters.isEmpty()) {
			progress.setCurrentChapter(chapters.getFirst());
		}
		progress.setProgressPercentage(MIN_PROGRESS);
		LocalDateTime now = LocalDateTime.now();
		progress.setStartedAt(now);
		progress.setLastReadAt(now);
		incrementReadingCount(book);
		return readingProgressRepository.save(progress);
	}

	private void applyProgress(ReadingProgress progress, Book book, ReadingProgressRequest request) {
		progress.setCurrentChapter(resolveCurrentChapter(book, request.currentChapterId()));
		progress.setProgressPercentage(normalizeProgress(request.progressPercentage()));
		progress.setLastReadAt(LocalDateTime.now());
	}

	private Chapter resolveCurrentChapter(Book book, UUID chapterId) {
		if (chapterId == null) {
			return null;
		}
		return chapterRepository.findByIdAndBook(chapterId, book)
			.orElseThrow(() -> new BusinessException("Current chapter must belong to this book"));
	}

	private BigDecimal normalizeProgress(BigDecimal progressPercentage) {
		BigDecimal progress = progressPercentage == null ? MIN_PROGRESS : progressPercentage;
		if (progress.compareTo(MIN_PROGRESS) < 0 || progress.compareTo(MAX_PROGRESS) > 0) {
			throw new BusinessException("Progress percentage must be between 0 and 100");
		}
		return progress.setScale(2, RoundingMode.HALF_UP);
	}

	private void incrementReadingCount(Book book) {
		book.setReadingCount(book.getReadingCount() + 1);
		bookRepository.save(book);
	}

	private Book getPublishedPublicBook(UUID bookId) {
		Book book = bookRepository.findByIdWithAuthor(bookId)
			.orElseThrow(() -> new ResourceNotFoundException("Book was not found"));
		if (book.getStatus() != BookStatus.PUBLISHED
			|| book.getVisibility() != BookVisibility.PUBLIC
			|| book.getPublishedAt() == null) {
			throw new BusinessException("Only published public books can be read");
		}
		return book;
	}
}
