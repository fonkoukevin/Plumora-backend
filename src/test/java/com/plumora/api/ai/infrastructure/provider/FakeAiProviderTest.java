package com.plumora.api.ai.infrastructure.provider;

import static org.assertj.core.api.Assertions.assertThat;

import com.plumora.api.ai.domain.AiWritingActionType;
import org.junit.jupiter.api.Test;

class FakeAiProviderTest {

	private final FakeAiProvider provider = new FakeAiProvider();

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
			assertThat(response.explanation()).contains("FakeAiProvider local");
			assertThat(response.explanation()).contains(actionType.name());
		}
	}
}
