package com.plumora.api.ai.presentation;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.UUID;

public record AiBookRecommendationItem(
	@JsonProperty("book_id") UUID bookId,
	String title,
	String reason,
	int score
) {
}
