package com.plumora.api.reading.presentation;

import java.time.LocalDateTime;
import java.util.UUID;

public record ExternalBookReviewResponse(
	UUID id,
	String externalId,
	String source,
	UUID userId,
	String username,
	int rating,
	String comment,
	LocalDateTime createdAt,
	LocalDateTime updatedAt
) {
}
