package com.plumora.api.book.presentation;

import com.plumora.api.book.domain.Book;
import com.plumora.api.book.domain.Chapter;
import com.plumora.api.book.domain.ChapterVersion;

public final class BookMapper {
	private BookMapper() {
	}

	public static BookResponse toResponse(Book book) {
		return new BookResponse(
			book.getId(),
			book.getAuthor().getId(),
			book.getAuthor().getUsername(),
			book.getTitle(),
			book.getSubtitle(),
			book.getSummary(),
			book.getCoverUrl(),
			book.getGenre(),
			book.getLanguageCode(),
			book.getStatus(),
			book.getVisibility(),
			book.getPublishedAt(),
			book.getReadingCount(),
			book.getAverageRating(),
			book.getCreatedAt(),
			book.getUpdatedAt()
		);
	}

	public static CatalogBookResponse toCatalogResponse(Book book) {
		return new CatalogBookResponse(
			book.getId(),
			book.getTitle(),
			book.getSubtitle(),
			book.getSummary(),
			book.getCoverUrl(),
			book.getGenre(),
			book.getLanguageCode(),
			book.getAuthor().getId(),
			book.getAuthor().getUsername(),
			authorDisplayName(book),
			book.getPublishedAt(),
			book.getReadingCount(),
			book.getAverageRating()
		);
	}

	public static CatalogBookDetailResponse toCatalogDetailResponse(Book book, long chapterCount) {
		return new CatalogBookDetailResponse(
			book.getId(),
			book.getTitle(),
			book.getSubtitle(),
			book.getSummary(),
			book.getCoverUrl(),
			book.getGenre(),
			book.getLanguageCode(),
			book.getAuthor().getId(),
			book.getAuthor().getUsername(),
			authorDisplayName(book),
			book.getPublishedAt(),
			book.getReadingCount(),
			book.getAverageRating(),
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
		String firstname = book.getAuthor().getFirstname() == null ? "" : book.getAuthor().getFirstname().trim();
		String lastname = book.getAuthor().getLastname() == null ? "" : book.getAuthor().getLastname().trim();
		String displayName = (firstname + " " + lastname).trim();
		return displayName.isBlank() ? book.getAuthor().getUsername() : displayName;
	}
}
