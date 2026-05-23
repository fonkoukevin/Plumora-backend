package com.plumora.api.ai.infrastructure;

import com.plumora.api.ai.domain.AiRecommendationRequestEntity;
import com.plumora.api.ai.domain.AiRecommendationResult;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AiRecommendationResultRepository extends JpaRepository<AiRecommendationResult, java.util.UUID> {
	@EntityGraph(attributePaths = {"book", "book.author", "request", "request.user"})
	List<AiRecommendationResult> findByRequestOrderByRankPositionAsc(AiRecommendationRequestEntity request);
}
