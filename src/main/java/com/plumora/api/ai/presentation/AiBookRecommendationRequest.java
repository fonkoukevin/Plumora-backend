package com.plumora.api.ai.presentation;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

public record AiBookRecommendationRequest(
	@JsonProperty("user_preferences") @Size(max = 1000) String userPreferences,
	@JsonProperty("favorite_genres") List<@Size(max = 80) String> favoriteGenres,
	@JsonProperty("reading_history_ids") List<UUID> readingHistoryIds,
	@Size(max = 10) String language,
	@Min(1) @Max(20) Integer limit
) {
}
