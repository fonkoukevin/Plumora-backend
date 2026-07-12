package com.plumora.api.ai.infrastructure.provider;

import java.util.List;

public record AiBetaReadingResult(
	String globalFeedback,
	List<String> strengths,
	List<String> weaknesses,
	int clarityScore,
	int rhythmScore,
	int coherenceScore,
	int characterScore,
	List<String> suggestions,
	List<String> warnings
) {
}
