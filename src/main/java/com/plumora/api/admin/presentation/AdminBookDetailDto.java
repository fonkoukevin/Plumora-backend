package com.plumora.api.admin.presentation;

import com.plumora.api.admin.domain.AdminBookType;
import com.plumora.api.book.domain.BookStatus;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record AdminBookDetailDto(
	UUID id,
	String title,
	List<String> authors,
	String summary,
	AdminBookType type,
	BookStatus status,
	String coverUrl,
	String readUrl,
	String source,
	String externalId,
	LocalDateTime createdAt,
	LocalDateTime updatedAt,
	long reportsCount,
	long chaptersCount
) {
}
