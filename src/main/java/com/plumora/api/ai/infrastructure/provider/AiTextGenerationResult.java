package com.plumora.api.ai.infrastructure.provider;

import java.util.List;

public record AiTextGenerationResult(
	String suggestion,
	String explanation,
	List<String> warnings
) {
}
