package com.plumora.api.ai.infrastructure.provider;

public record AiBetaReadingPrompt(
	String text,
	String language,
	String genre,
	String expectedFeedbackLevel
) {
}
