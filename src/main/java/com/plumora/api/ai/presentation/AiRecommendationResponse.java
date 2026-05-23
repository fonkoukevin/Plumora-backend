package com.plumora.api.ai.presentation;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record AiRecommendationResponse(
	UUID id,
	String queryText,
	String mood,
	String preferredDuration,
	String preferredGenre,
	LocalDateTime createdAt,
	List<RecommendedBookResponse> recommendations
) {
}
