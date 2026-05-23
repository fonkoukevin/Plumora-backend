package com.plumora.api.ai.presentation;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AiRecommendationRequest(
	@JsonProperty("query_text")
	@NotBlank @Size(max = 2000) String queryText,
	@Size(max = 40) String mood,
	@JsonProperty("preferred_duration")
	@Size(max = 30) String preferredDuration,
	@JsonProperty("preferred_genre")
	@Size(max = 80) String preferredGenre
) {
}
