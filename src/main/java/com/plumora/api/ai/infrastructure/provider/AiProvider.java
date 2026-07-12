package com.plumora.api.ai.infrastructure.provider;

public interface AiProvider {
	AiProviderResponse generateWritingSuggestion(AiWritingPrompt prompt);

	AiTextGenerationResult rewriteText(AiTextGenerationPrompt prompt);

	AiTextGenerationResult summarizeText(AiTextGenerationPrompt prompt);

	AiTextGenerationResult continueText(AiTextGenerationPrompt prompt);

	AiTitleSuggestionResult suggestTitles(AiTextGenerationPrompt prompt);

	AiBetaReadingResult analyzeForBetaReading(AiBetaReadingPrompt prompt);

	String providerName();

	String modelName();
}
