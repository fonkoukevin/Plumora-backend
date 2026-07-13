package com.plumora.api.admin.presentation;

import java.time.LocalDateTime;

public record AdminAiStatusDto(
	boolean enabled,
	LocalDateTime updatedAt,
	String providerName,
	String modelName,
	long totalWritingRequests,
	long totalRecommendationRequests
) {
}
