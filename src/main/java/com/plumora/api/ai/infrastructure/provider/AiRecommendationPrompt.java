package com.plumora.api.ai.infrastructure.provider;

public record AiRecommendationPrompt(
	String queryText,
	String mood,
	String preferredDuration,
	String preferredGenre
) {
}
