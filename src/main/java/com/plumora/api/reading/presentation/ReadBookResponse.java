package com.plumora.api.reading.presentation;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record ReadBookResponse(
	UUID id,
	String title,
	String subtitle,
	String summary,
	String coverUrl,
	String genre,
	String languageCode,
	String authorUsername,
	int readingCount,
	BigDecimal averageRating,
	String externalSource,
	String externalId,
	List<String> externalAuthors,
	String sourceUrl,
	String readUrl,
	ReadingProgressResponse progress,
	List<ReadChapterResponse> chapters
) {
}
