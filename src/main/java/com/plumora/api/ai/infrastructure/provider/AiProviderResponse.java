package com.plumora.api.ai.infrastructure.provider;

public record AiProviderResponse(
	String suggestionText,
	String explanation
) {
}
