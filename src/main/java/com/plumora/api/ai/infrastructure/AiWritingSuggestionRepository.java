package com.plumora.api.ai.infrastructure;

import com.plumora.api.ai.domain.AiWritingRequest;
import com.plumora.api.ai.domain.AiWritingSuggestion;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AiWritingSuggestionRepository extends JpaRepository<AiWritingSuggestion, UUID> {
	@EntityGraph(attributePaths = {"request", "request.user", "request.chapter", "request.chapter.book", "request.chapter.book.author"})
	List<AiWritingSuggestion> findByRequestOrderByCreatedAtDesc(AiWritingRequest request);

	@EntityGraph(attributePaths = {"request", "request.user", "request.chapter", "request.chapter.book", "request.chapter.book.author"})
	@Query("select s from AiWritingSuggestion s where s.id = :id")
	Optional<AiWritingSuggestion> findByIdWithRequestDetails(@Param("id") UUID id);
}
