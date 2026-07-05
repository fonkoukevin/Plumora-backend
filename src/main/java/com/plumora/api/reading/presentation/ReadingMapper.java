package com.plumora.api.reading.presentation;

import com.plumora.api.book.domain.Chapter;
import com.plumora.api.book.presentation.BookCoverUrlMapper;
import com.plumora.api.reading.application.ReadSession;
import com.plumora.api.reading.domain.ExternalBookReview;
import com.plumora.api.reading.domain.Favorite;
import com.plumora.api.reading.domain.ReadingProgress;
import com.plumora.api.reading.domain.Review;

public final class ReadingMapper {
	private ReadingMapper() {
	}

	public static ReadBookResponse toReadBookResponse(ReadSession session) {
		return new ReadBookResponse(
			session.book().getId(),
			session.book().getTitle(),
			session.book().getSubtitle(),
			session.book().getSummary(),
			BookCoverUrlMapper.toResponseCoverUrl(session.book().getCoverUrl()),
			session.book().getGenre(),
			session.book().getLanguageCode(),
			session.book().getAuthor().getUsername(),
			session.book().getReadingCount(),
			session.book().getAverageRating(),
			session.book().getExternalSource() == null ? null : session.book().getExternalSource().name(),
			session.book().getExternalId(),
			session.book().getExternalAuthors() == null ? java.util.List.of() : session.book().getExternalAuthors(),
			session.book().getSourceUrl(),
			session.book().getReadUrl(),
			toProgressResponse(session.progress()),
			session.chapters().stream()
				.map(ReadingMapper::toReadChapterResponse)
				.toList()
		);
	}

	public static ReadingProgressResponse toProgressResponse(ReadingProgress progress) {
		return new ReadingProgressResponse(
			progress.getId(),
			progress.getBook().getId(),
			progress.getBook().getTitle(),
			BookCoverUrlMapper.toResponseCoverUrl(progress.getBook().getCoverUrl()),
			progress.getCurrentChapter() == null ? null : progress.getCurrentChapter().getId(),
			progress.getCurrentChapter() == null ? null : progress.getCurrentChapter().getTitle(),
			progress.getProgressPercentage(),
			progress.getStartedAt(),
			progress.getLastReadAt(),
			progress.getFinishedAt()
		);
	}

	public static FavoriteResponse toFavoriteResponse(Favorite favorite) {
		return new FavoriteResponse(
			favorite.getId(),
			favorite.getBook().getId(),
			favorite.getBook().getTitle(),
			BookCoverUrlMapper.toResponseCoverUrl(favorite.getBook().getCoverUrl()),
			favorite.getBook().getAuthor().getUsername(),
			favorite.getCreatedAt()
		);
	}

	public static ReviewResponse toReviewResponse(Review review) {
		return new ReviewResponse(
			review.getId(),
			review.getBook().getId(),
			review.getBook().getTitle(),
			BookCoverUrlMapper.toResponseCoverUrl(review.getBook().getCoverUrl()),
			review.getUser().getId(),
			review.getUser().getUsername(),
			review.getRating(),
			review.getComment(),
			review.getCreatedAt(),
			review.getUpdatedAt()
		);
	}

	public static ExternalBookReviewResponse toExternalBookReviewResponse(ExternalBookReview review) {
		return new ExternalBookReviewResponse(
			review.getId(),
			review.getExternalId(),
			review.getExternalSource().name(),
			review.getUser().getId(),
			review.getUser().getUsername(),
			review.getRating(),
			review.getComment(),
			review.getCreatedAt(),
			review.getUpdatedAt()
		);
	}

	private static ReadChapterResponse toReadChapterResponse(Chapter chapter) {
		return new ReadChapterResponse(
			chapter.getId(),
			chapter.getTitle(),
			chapter.getContent(),
			chapter.getChapterOrder(),
			chapter.getWordCount()
		);
	}
}
