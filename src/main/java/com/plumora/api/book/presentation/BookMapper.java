package com.plumora.api.book.presentation;

import com.plumora.api.book.application.ChapterStats;
import com.plumora.api.book.domain.Book;
import com.plumora.api.book.domain.Chapter;
import com.plumora.api.book.domain.ChapterVersion;
import java.util.List;
import java.util.Map;

public final class BookMapper {
	private BookMapper() {
	}

	public static BookResponse toResponse(Book book) {
		return toResponse(book, ChapterStats.EMPTY);
	}

	public static BookResponse toResponse(Book book, ChapterStats stats) {
		return new BookResponse(
			book.getId(),
			book.getAuthor().getId(),
			book.getAuthor().getUsername(),
			book.getTitle(),
			book.getSubtitle(),
			book.getSummary(),
			BookCoverUrlMapper.toResponseCoverUrl(book.getCoverUrl()),
			book.getGenre(),
			book.getLanguageCode(),
			book.getStatus(),
			book.getVisibility(),
			book.getPublishedAt(),
			book.getReadingCount(),
			book.getAverageRating(),
			sourceName(book),
			book.getExternalId(),
			externalAuthors(book),
			book.getSourceUrl(),
			book.getReadUrl(),
			book.getDownloadCount(),
			book.getCreatedAt(),
			book.getUpdatedAt(),
			stats.chapterCount(),
			stats.wordCount()
		);
	}

	public static CatalogBookResponse toCatalogResponse(Book book) {
		return new CatalogBookResponse(
			book.getId(),
			book.getTitle(),
			book.getSubtitle(),
			book.getSummary(),
			BookCoverUrlMapper.toResponseCoverUrl(book.getCoverUrl()),
			book.getGenre(),
			book.getLanguageCode(),
			book.getAuthor().getId(),
			book.getAuthor().getUsername(),
			authorDisplayName(book),
			book.getPublishedAt(),
			book.getReadingCount(),
			book.getAverageRating(),
			sourceName(book),
			book.getExternalId(),
			externalAuthors(book),
			book.getSourceUrl(),
			book.getReadUrl(),
			book.getDownloadCount()
		);
	}

	public static CatalogBookDetailResponse toCatalogDetailResponse(Book book, long chapterCount) {
		return new CatalogBookDetailResponse(
			book.getId(),
			book.getTitle(),
			book.getSubtitle(),
			book.getSummary(),
			BookCoverUrlMapper.toResponseCoverUrl(book.getCoverUrl()),
			book.getGenre(),
			book.getLanguageCode(),
			book.getAuthor().getId(),
			book.getAuthor().getUsername(),
			authorDisplayName(book),
			book.getPublishedAt(),
			book.getReadingCount(),
			book.getAverageRating(),
			sourceName(book),
			book.getExternalId(),
			externalAuthors(book),
			book.getSourceUrl(),
			book.getReadUrl(),
			book.getDownloadCount(),
			formats(book),
			chapterCount
		);
	}

	public static ChapterResponse toResponse(Chapter chapter) {
		return new ChapterResponse(
			chapter.getId(),
			chapter.getBook().getId(),
			chapter.getTitle(),
			chapter.getContent(),
			chapter.getChapterOrder(),
			chapter.getWordCount(),
			chapter.getCreatedAt(),
			chapter.getUpdatedAt()
		);
	}

	public static ChapterVersionResponse toResponse(ChapterVersion version) {
		return new ChapterVersionResponse(
			version.getId(),
			version.getChapter().getId(),
			version.getCreatedByUser().getId(),
			version.getCreatedByUser().getUsername(),
			version.getVersionNumber(),
			version.getContentSnapshot(),
			version.getCreatedAt()
		);
	}

	private static String authorDisplayName(Book book) {
		List<String> externalAuthors = externalAuthors(book);
		if (!externalAuthors.isEmpty()) {
			return String.join(", ", externalAuthors);
		}
		String firstname = book.getAuthor().getFirstname() == null ? "" : book.getAuthor().getFirstname().trim();
		String lastname = book.getAuthor().getLastname() == null ? "" : book.getAuthor().getLastname().trim();
		String displayName = (firstname + " " + lastname).trim();
		return displayName.isBlank() ? book.getAuthor().getUsername() : displayName;
	}

	private static String sourceName(Book book) {
		return book.getExternalSource() == null ? null : book.getExternalSource().name();
	}

	private static List<String> externalAuthors(Book book) {
		return book.getExternalAuthors() == null ? List.of() : book.getExternalAuthors();
	}

	private static Map<String, String> formats(Book book) {
		return book.getFormatsJson() == null ? Map.of() : book.getFormatsJson();
	}
}
