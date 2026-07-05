package com.plumora.api.ai.presentation;

import com.plumora.api.ai.application.AiRecommendationService.RecommendationBundle;
import com.plumora.api.ai.domain.AiRecommendationResult;
import com.plumora.api.book.presentation.BookCoverUrlMapper;

public final class AiRecommendationMapper {
	private AiRecommendationMapper() {
	}

	public static AiRecommendationResponse toResponse(RecommendationBundle bundle) {
		return new AiRecommendationResponse(
			bundle.request().getId(),
			bundle.request().getQueryText(),
			bundle.request().getMood(),
			bundle.request().getPreferredDuration(),
			bundle.request().getPreferredGenre(),
			bundle.request().getCreatedAt(),
			bundle.results().stream()
				.map(AiRecommendationMapper::toRecommendedBookResponse)
				.toList()
		);
	}

	private static RecommendedBookResponse toRecommendedBookResponse(AiRecommendationResult result) {
		return new RecommendedBookResponse(
			result.getBook().getId(),
			result.getBook().getTitle(),
			BookCoverUrlMapper.toResponseCoverUrl(result.getBook().getCoverUrl()),
			result.getMatchScore(),
			result.getReasons(),
			result.getRankPosition()
		);
	}
}
