package com.plumora.api.ai.presentation;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;
import java.util.List;

public record AiTextGenerationResponse(
	String suggestion,
	String explanation,
	List<String> warnings,
	String provider,
	String model,
	@JsonProperty("generated_at") LocalDateTime generatedAt
) {
}
