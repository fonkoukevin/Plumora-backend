package com.plumora.api.ai.presentation;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.plumora.api.ai.domain.AiWritingActionType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record CreateAiWritingSuggestionRequest(
	@JsonProperty("chapter_id")
	@NotNull UUID chapterId,
	@JsonProperty("selected_text")
	@NotBlank @Size(max = 5000) String selectedText,
	@JsonProperty("context_text")
	@Size(max = 10000) String contextText,
	@JsonProperty("action_type")
	@NotNull AiWritingActionType actionType
) {
}
