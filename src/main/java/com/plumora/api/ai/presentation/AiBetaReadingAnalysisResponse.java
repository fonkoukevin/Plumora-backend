package com.plumora.api.ai.presentation;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;
import java.util.List;

public record AiBetaReadingAnalysisResponse(
	@JsonProperty("global_feedback") String globalFeedback,
	List<String> strengths,
	List<String> weaknesses,
	@JsonProperty("clarity_score") int clarityScore,
	@JsonProperty("rhythm_score") int rhythmScore,
	@JsonProperty("coherence_score") int coherenceScore,
	@JsonProperty("character_score") int characterScore,
	List<String> suggestions,
	List<String> warnings,
	String provider,
	String model,
	@JsonProperty("generated_at") LocalDateTime generatedAt
) {
}
