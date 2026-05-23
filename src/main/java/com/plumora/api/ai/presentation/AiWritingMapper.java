package com.plumora.api.ai.presentation;

import com.plumora.api.ai.domain.AiWritingRequest;
import com.plumora.api.ai.domain.AiWritingSuggestion;
import java.util.List;

public final class AiWritingMapper {
	private AiWritingMapper() {
	}

	public static AiWritingSuggestionResponse toSuggestionResponse(AiWritingSuggestion suggestion) {
		AiWritingRequest request = suggestion.getRequest();
		return new AiWritingSuggestionResponse(
			suggestion.getId(),
			request.getId(),
			request.getChapter().getId(),
			request.getChapter().getTitle(),
			request.getChapter().getBook().getId(),
			request.getChapter().getBook().getTitle(),
			request.getChapter().getBook().getCoverUrl(),
			suggestion.getSuggestionText(),
			suggestion.getExplanation(),
			suggestion.getStatus(),
			suggestion.getCreatedAt()
		);
	}

	public static AiWritingRequestResponse toRequestResponse(
		AiWritingRequest request,
		List<AiWritingSuggestion> suggestions
	) {
		return new AiWritingRequestResponse(
			request.getId(),
			request.getChapter().getId(),
			request.getChapter().getTitle(),
			request.getChapter().getBook().getId(),
			request.getChapter().getBook().getTitle(),
			request.getChapter().getBook().getCoverUrl(),
			request.getSelectedText(),
			request.getContextText(),
			request.getActionType(),
			request.getCreatedAt(),
			suggestions.stream()
				.map(AiWritingMapper::toSuggestionResponse)
				.toList()
		);
	}
}
