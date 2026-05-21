package com.plumora.api.reading.presentation;

import java.time.LocalDateTime;
import java.util.UUID;

public record ReviewResponse(
	UUID id,
	UUID bookId,
	String bookTitle,
	UUID userId,
	String username,
	int rating,
	String comment,
	LocalDateTime createdAt,
	LocalDateTime updatedAt
) {
}
