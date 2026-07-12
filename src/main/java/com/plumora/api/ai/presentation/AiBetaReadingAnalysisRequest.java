package com.plumora.api.ai.presentation;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record AiBetaReadingAnalysisRequest(
	@NotBlank @Size(max = 20000) String text,
	@Size(max = 10) String language,
	@Size(max = 80) String genre,
	@JsonProperty("expected_feedback_level") @Size(max = 30) String expectedFeedbackLevel,
	@JsonProperty("manuscript_id") UUID manuscriptId,
	@JsonProperty("chapter_id") UUID chapterId
) {
}
