package com.plumora.api.book.application;

import com.plumora.api.book.domain.Book;
import com.plumora.api.book.domain.Chapter;
import com.plumora.api.book.domain.ChapterVersion;
import com.plumora.api.book.infrastructure.ChapterRepository;
import com.plumora.api.book.infrastructure.ChapterVersionRepository;
import com.plumora.api.shared.exception.ResourceNotFoundException;
import com.plumora.api.user.application.UserService;
import com.plumora.api.user.domain.User;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ChapterVersionService {

	private final ChapterRepository chapterRepository;
	private final ChapterVersionRepository chapterVersionRepository;
	private final BookService bookService;
	private final UserService userService;

	public ChapterVersionService(
		ChapterRepository chapterRepository,
		ChapterVersionRepository chapterVersionRepository,
		BookService bookService,
		UserService userService
	) {
		this.chapterRepository = chapterRepository;
		this.chapterVersionRepository = chapterVersionRepository;
		this.bookService = bookService;
		this.userService = userService;
	}

	@Transactional
	public ChapterVersion createVersion(String currentUserEmail, UUID chapterId) {
		User currentUser = userService.getCurrentUser(currentUserEmail);
		Chapter chapter = getOwnedEditableChapter(currentUserEmail, chapterId);
		return createVersionSnapshot(chapter, currentUser);
	}

	@Transactional(readOnly = true)
	public List<ChapterVersion> getChapterVersions(String currentUserEmail, UUID chapterId) {
		Chapter chapter = findChapter(chapterId);
		bookService.getOwnedBook(currentUserEmail, chapter.getBook().getId());
		return chapterVersionRepository.findByChapterOrderByVersionNumberDesc(chapter);
	}

	@Transactional(readOnly = true)
	public ChapterVersion getVersion(String currentUserEmail, UUID versionId) {
		ChapterVersion version = findVersion(versionId);
		bookService.getOwnedBook(currentUserEmail, version.getChapter().getBook().getId());
		return version;
	}

	@Transactional
	public RestoreResult restoreVersion(String currentUserEmail, UUID versionId) {
		User currentUser = userService.getCurrentUser(currentUserEmail);
		ChapterVersion version = findVersion(versionId);
		Chapter chapter = version.getChapter();
		Book book = bookService.getOwnedEditableBook(currentUserEmail, chapter.getBook().getId());
		bookService.ensureEditable(book);

		ChapterVersion backupVersion = null;
		String currentContent = normalizeContent(chapter.getContent());
		String targetContent = normalizeContent(version.getContentSnapshot());
		if (!Objects.equals(currentContent, targetContent)) {
			backupVersion = createVersionSnapshot(chapter, currentUser);
		}

		chapter.setContent(version.getContentSnapshot());
		chapter.updateWordCount();
		Chapter restoredChapter = chapterRepository.save(chapter);
		return new RestoreResult(restoredChapter, version, backupVersion);
	}

	private ChapterVersion createVersionSnapshot(Chapter chapter, User currentUser) {
		ChapterVersion version = new ChapterVersion();
		version.setChapter(chapter);
		version.setCreatedByUser(currentUser);
		version.setVersionNumber(nextVersionNumber(chapter));
		version.setContentSnapshot(normalizeContent(chapter.getContent()));
		return chapterVersionRepository.save(version);
	}

	private int nextVersionNumber(Chapter chapter) {
		return chapterVersionRepository.findMaxVersionNumberByChapter(chapter) + 1;
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

	private ChapterVersion findVersion(UUID versionId) {
		return chapterVersionRepository.findById(versionId)
			.orElseThrow(() -> new ResourceNotFoundException("Chapter version was not found"));
	}

	private String normalizeContent(String content) {
		return content == null ? "" : content;
	}

	public record RestoreResult(
		Chapter chapter,
		ChapterVersion restoredVersion,
		ChapterVersion backupVersion
	) {
	}
}
