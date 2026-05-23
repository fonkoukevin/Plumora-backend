package com.plumora.api.ai.presentation;

import com.plumora.api.ai.domain.AiWritingActionType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record AiWritingRequestResponse(
	UUID id,
	UUID chapterId,
	String chapterTitle,
	UUID bookId,
	String bookTitle,
	String selectedText,
	String contextText,
	AiWritingActionType actionType,
	LocalDateTime createdAt,
	List<AiWritingSuggestionResponse> suggestions
) {
}
