package com.plumora.api.book.presentation;

import java.time.LocalDateTime;
import java.util.UUID;

public record ChapterResponse(
	UUID id,
	UUID bookId,
	String title,
	String content,
	int chapterOrder,
	int wordCount,
	LocalDateTime createdAt,
	LocalDateTime updatedAt
) {
}
