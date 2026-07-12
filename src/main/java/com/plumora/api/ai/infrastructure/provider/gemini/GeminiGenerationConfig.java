package com.plumora.api.ai.infrastructure.provider.gemini;

import com.fasterxml.jackson.annotation.JsonProperty;

public record GeminiGenerationConfig(
	@JsonProperty("responseMimeType") String responseMimeType,
	Double temperature
) {
}
