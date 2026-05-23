package com.plumora.api.ai.infrastructure;

import com.plumora.api.ai.domain.AiWritingRequest;
import com.plumora.api.user.domain.User;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AiWritingRequestRepository extends JpaRepository<AiWritingRequest, UUID> {
	@EntityGraph(attributePaths = {"user", "chapter", "chapter.book", "chapter.book.author"})
	List<AiWritingRequest> findByUserOrderByCreatedAtDesc(User user);

	@EntityGraph(attributePaths = {"user", "chapter", "chapter.book", "chapter.book.author"})
	@Query("select r from AiWritingRequest r where r.id = :id")
	Optional<AiWritingRequest> findByIdWithDetails(@Param("id") UUID id);
}
