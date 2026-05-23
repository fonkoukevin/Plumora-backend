package com.plumora.api.ai.infrastructure.provider;

public interface AiProvider {
	AiProviderResponse generateWritingSuggestion(AiWritingPrompt prompt);
}
