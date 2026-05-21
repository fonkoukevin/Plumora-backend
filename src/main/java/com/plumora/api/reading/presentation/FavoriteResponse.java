package com.plumora.api.reading.presentation;

import java.time.LocalDateTime;
import java.util.UUID;

public record FavoriteResponse(
	UUID id,
	UUID bookId,
	String bookTitle,
	String bookCoverUrl,
	String authorUsername,
	LocalDateTime createdAt
) {
}
