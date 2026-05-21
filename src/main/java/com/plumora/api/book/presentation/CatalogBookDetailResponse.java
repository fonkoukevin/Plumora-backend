package com.plumora.api.book.presentation;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record CatalogBookDetailResponse(
	UUID id,
	String title,
	String subtitle,
	String summary,
	String coverUrl,
	String genre,
	String languageCode,
	UUID authorId,
	String authorUsername,
	String authorDisplayName,
	LocalDateTime publishedAt,
	int readingCount,
	BigDecimal averageRating,
	long chapterCount
) {
}
