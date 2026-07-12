package com.plumora.api.ai.presentation;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;
import java.util.List;

public record AiBookRecommendationResponse(
	List<AiBookRecommendationItem> recommendations,
	String provider,
	String model,
	@JsonProperty("generated_at") LocalDateTime generatedAt
) {
}
