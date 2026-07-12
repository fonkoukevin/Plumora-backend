package com.plumora.api.ai.presentation;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record AiTextGenerationRequest(
	@NotBlank @Size(max = 20000) String text,
	@Size(max = 10) String language,
	@Size(max = 50) String tone,
	@Size(max = 2000) String instruction,
	@JsonProperty("manuscript_id") UUID manuscriptId,
	@JsonProperty("chapter_id") UUID chapterId
) {
}
