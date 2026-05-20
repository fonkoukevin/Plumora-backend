package com.plumora.api.book.presentation;

import com.plumora.api.book.domain.Book;
import com.plumora.api.book.domain.Chapter;

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
}
