package com.plumora.api.ai.infrastructure.provider;

import java.util.List;

public record AiTitleSuggestionResult(
	List<String> titles,
	String explanation,
	List<String> warnings
) {
}
