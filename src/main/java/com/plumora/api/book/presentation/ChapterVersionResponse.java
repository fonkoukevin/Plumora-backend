package com.plumora.api.book.presentation;

import java.time.LocalDateTime;
import java.util.UUID;

public record ChapterVersionResponse(
	UUID id,
	UUID chapterId,
	UUID createdByUserId,
	String createdByUsername,
	int versionNumber,
	String contentSnapshot,
	LocalDateTime createdAt
) {
}
