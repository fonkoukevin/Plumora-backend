package com.plumora.api.ai.presentation;

import com.plumora.api.ai.domain.AiSuggestionStatus;
import java.time.LocalDateTime;
import java.util.UUID;

public record AiWritingSuggestionResponse(
	UUID id,
	UUID requestId,
	UUID chapterId,
	String chapterTitle,
	UUID bookId,
	String bookTitle,
	String bookCoverUrl,
	String suggestionText,
	String explanation,
	AiSuggestionStatus status,
	LocalDateTime createdAt
) {
}
