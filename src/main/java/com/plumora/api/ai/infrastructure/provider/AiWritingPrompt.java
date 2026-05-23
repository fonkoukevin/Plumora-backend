package com.plumora.api.ai.infrastructure.provider;

import com.plumora.api.ai.domain.AiWritingActionType;

public record AiWritingPrompt(
	String selectedText,
	String contextText,
	AiWritingActionType actionType,
	String chapterTitle,
	String bookTitle
) {
}
