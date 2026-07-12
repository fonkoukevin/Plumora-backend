package com.plumora.api.ai.infrastructure.provider.gemini;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record GeminiGenerateContentRequest(
	@JsonProperty("system_instruction") GeminiSystemInstruction systemInstruction,
	List<GeminiContent> contents,
	GeminiGenerationConfig generationConfig
) {
}
