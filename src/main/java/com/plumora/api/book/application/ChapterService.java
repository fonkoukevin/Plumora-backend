package com.plumora.api.book.application;

import com.plumora.api.book.domain.Book;
import com.plumora.api.book.domain.Chapter;
import com.plumora.api.book.infrastructure.ChapterRepository;
import com.plumora.api.book.presentation.CreateChapterRequest;
import com.plumora.api.book.presentation.UpdateChapterRequest;
import com.plumora.api.shared.exception.BusinessException;
import com.plumora.api.shared.exception.ResourceNotFoundException;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ChapterService {

	private final ChapterRepository chapterRepository;
	private final BookService bookService;

	public ChapterService(ChapterRepository chapterRepository, BookService bookService) {
		this.chapterRepository = chapterRepository;
		this.bookService = bookService;
	}

	@Transactional
	public Chapter createChapter(String currentUserEmail, UUID bookId, CreateChapterRequest request) {
		Book book = bookService.getOwnedEditableBook(currentUserEmail, bookId);
		ensureChapterOrderAvailable(book, request.chapterOrder());

		Chapter chapter = new Chapter();
		chapter.setBook(book);
		chapter.setTitle(request.title());
		chapter.setContent(request.content());
		chapter.setChapterOrder(request.chapterOrder());
		chapter.updateWordCount();
		return chapterRepository.save(chapter);
	}

	@Transactional(readOnly = true)
	public List<Chapter> getBookChapters(String currentUserEmail, UUID bookId) {
		Book book = bookService.getOwnedBook(currentUserEmail, bookId);
		return chapterRepository.findByBookOrderByChapterOrderAsc(book);
	}

	@Transactional(readOnly = true)
	public Chapter getChapter(String currentUserEmail, UUID chapterId) {
		Chapter chapter = findChapter(chapterId);
		bookService.getOwnedBook(currentUserEmail, chapter.getBook().getId());
		return chapter;
	}

	@Transactional
	public Chapter updateChapter(String currentUserEmail, UUID chapterId, UpdateChapterRequest request) {
		Chapter chapter = getOwnedEditableChapter(currentUserEmail, chapterId);
		chapter.setTitle(request.title());
		chapter.setContent(request.content());
		chapter.updateWordCount();
		return chapterRepository.save(chapter);
	}

	@Transactional
	public Chapter updateChapterOrder(String currentUserEmail, UUID chapterId, int chapterOrder) {
		Chapter chapter = getOwnedEditableChapter(currentUserEmail, chapterId);
		if (chapter.getChapterOrder() != chapterOrder) {
			ensureChapterOrderAvailable(chapter.getBook(), chapterOrder);
			chapter.setChapterOrder(chapterOrder);
		}
		return chapterRepository.save(chapter);
	}

	@Transactional
	public void deleteChapter(String currentUserEmail, UUID chapterId) {
		Chapter chapter = getOwnedEditableChapter(currentUserEmail, chapterId);
		chapterRepository.delete(chapter);
	}

	private Chapter getOwnedEditableChapter(String currentUserEmail, UUID chapterId) {
		Chapter chapter = findChapter(chapterId);
		bookService.getOwnedEditableBook(currentUserEmail, chapter.getBook().getId());
		return chapter;
	}

	private Chapter findChapter(UUID chapterId) {
		return chapterRepository.findById(chapterId)
			.orElseThrow(() -> new ResourceNotFoundException("Chapter was not found"));
	}

	private void ensureChapterOrderAvailable(Book book, int chapterOrder) {
		if (chapterRepository.existsByBookAndChapterOrder(book, chapterOrder)) {
			throw new BusinessException("Chapter order is already used for this book");
		}
	}
}
