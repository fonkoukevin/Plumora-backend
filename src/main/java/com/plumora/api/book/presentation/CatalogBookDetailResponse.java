package com.plumora.api.book.presentation;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
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
	String externalSource,
	String externalId,
	List<String> externalAuthors,
	String sourceUrl,
	String readUrl,
	Integer downloadCount,
	Map<String, String> formats,
	long chapterCount
) {
}
