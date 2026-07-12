package com.plumora.api.ai.infrastructure.provider;

import static org.assertj.core.api.Assertions.assertThat;

import com.plumora.api.ai.domain.AiWritingActionType;
import org.junit.jupiter.api.Test;

class MockAiProviderTest {

	private final MockAiProvider provider = new MockAiProvider();

	@Test
	void generatesLocalWritingSuggestionForEveryActionType() {
		for (AiWritingActionType actionType : AiWritingActionType.values()) {
			AiProviderResponse response = provider.generateWritingSuggestion(new AiWritingPrompt(
				"Je ne sais pas tres bien quoi dire.",
				"Dialogue dans une scene tendue.",
				actionType,
				"Chapitre 1",
				"Livre test"
			));

			assertThat(response.suggestionText()).isNotBlank();
			assertThat(response.explanation()).contains("MockAiProvider local");
			assertThat(response.explanation()).contains(actionType.name());
		}
	}

	@Test
	void rewriteTextReturnsNonBlankSuggestion() {
		AiTextGenerationResult result = provider.rewriteText(new AiTextGenerationPrompt(
			"Le vent etait tres froid ce soir-la.",
			"fr",
			"neutre",
			null,
			null
		));

		assertThat(result.suggestion()).isNotBlank();
		assertThat(result.explanation()).contains("MockAiProvider local");
	}

	@Test
	void summarizeTextReturnsNonBlankSuggestion() {
		AiTextGenerationResult result = provider.summarizeText(new AiTextGenerationPrompt(
			"Un long texte a resumer pour verifier le comportement du mock.",
			"fr",
			null,
			null,
			null
		));

		assertThat(result.suggestion()).isNotBlank();
	}

	@Test
	void continueTextReturnsNonBlankSuggestion() {
		AiTextGenerationResult result = provider.continueText(new AiTextGenerationPrompt(
			"Elle ouvrit la porte lentement.",
			"fr",
			null,
			null,
			null
		));

		assertThat(result.suggestion()).isNotBlank();
	}

	@Test
	void suggestTitlesReturnsThreeNonBlankTitles() {
		AiTitleSuggestionResult result = provider.suggestTitles(new AiTextGenerationPrompt(
			"Une histoire de phare abandonne sur une ile bretonne.",
			"fr",
			null,
			null,
			null
		));

		assertThat(result.titles()).hasSize(3);
		assertThat(result.titles()).allMatch(title -> !title.isBlank());
	}

	@Test
	void analyzeForBetaReadingReturnsScoresBetweenZeroAndTen() {
		AiBetaReadingResult result = provider.analyzeForBetaReading(new AiBetaReadingPrompt(
			"Un extrait de roman fantastique.",
			"fr",
			"Fantasy",
			"detaille"
		));

		assertThat(result.clarityScore()).isBetween(0, 10);
		assertThat(result.rhythmScore()).isBetween(0, 10);
		assertThat(result.coherenceScore()).isBetween(0, 10);
		assertThat(result.characterScore()).isBetween(0, 10);
		assertThat(result.globalFeedback()).isNotBlank();
	}
}
