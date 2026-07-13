package com.plumora.api.admin.presentation;

import com.plumora.api.admin.domain.AdminBookType;
import com.plumora.api.book.domain.BookStatus;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record AdminBookListDto(
	UUID id,
	String title,
	List<String> authors,
	AdminBookType type,
	BookStatus status,
	String coverUrl,
	LocalDateTime createdAt,
	String source,
	String externalId,
	long reportsCount
) {
}
