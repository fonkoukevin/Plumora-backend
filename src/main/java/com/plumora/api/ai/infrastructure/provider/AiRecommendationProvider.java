package com.plumora.api.ai.infrastructure.provider;

import com.plumora.api.book.domain.Book;
import java.util.List;

public interface AiRecommendationProvider {
	List<AiRecommendationCandidate> recommendBooks(AiRecommendationPrompt prompt, List<Book> candidates);
}
