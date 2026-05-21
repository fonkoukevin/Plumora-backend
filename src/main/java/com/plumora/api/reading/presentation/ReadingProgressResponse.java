package com.plumora.api.reading.presentation;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record ReadingProgressResponse(
	UUID id,
	UUID bookId,
	String bookTitle,
	String bookCoverUrl,
	UUID currentChapterId,
	String currentChapterTitle,
	BigDecimal progressPercentage,
	LocalDateTime startedAt,
	LocalDateTime lastReadAt,
	LocalDateTime finishedAt
) {
}
