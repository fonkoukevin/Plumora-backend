package com.plumora.api.ai.infrastructure.provider;

import com.plumora.api.book.domain.Book;
import java.util.List;

public record AiRecommendationCandidate(
	Book book,
	int matchScore,
	List<String> reasons
) {
}
