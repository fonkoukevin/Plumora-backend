package com.plumora.api.ai.infrastructure.provider;

public record AiTextGenerationPrompt(
	String text,
	String language,
	String tone,
	String instruction,
	String contextTitle
) {
}
