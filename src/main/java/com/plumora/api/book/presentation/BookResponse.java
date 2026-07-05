package com.plumora.api.book.presentation;

import com.plumora.api.book.domain.BookStatus;
import com.plumora.api.book.domain.BookVisibility;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record BookResponse(
	UUID id,
	UUID authorId,
	String authorUsername,
	String title,
	String subtitle,
	String summary,
	String coverUrl,
	String genre,
	String languageCode,
	BookStatus status,
	BookVisibility visibility,
	LocalDateTime publishedAt,
	int readingCount,
	BigDecimal averageRating,
	String externalSource,
	String externalId,
	List<String> externalAuthors,
	String sourceUrl,
	String readUrl,
	Integer downloadCount,
	LocalDateTime createdAt,
	LocalDateTime updatedAt
) {
}
