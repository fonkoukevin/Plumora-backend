package com.plumora.api.ai.presentation;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.UUID;

public record RecommendedBookResponse(
	@JsonProperty("book_id")
	UUID bookId,
	String title,
	String coverUrl,
	@JsonProperty("match_score")
	int matchScore,
	List<String> reasons,
	@JsonProperty("rank_position")
	int rankPosition
) {
}
