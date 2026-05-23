package com.plumora.api.ai.infrastructure;

import com.plumora.api.ai.domain.AiRecommendationRequestEntity;
import com.plumora.api.user.domain.User;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AiRecommendationRequestRepository extends JpaRepository<AiRecommendationRequestEntity, UUID> {
	@EntityGraph(attributePaths = "user")
	List<AiRecommendationRequestEntity> findByUserOrderByCreatedAtDesc(User user);

	@EntityGraph(attributePaths = "user")
	@Query("select r from AiRecommendationRequestEntity r where r.id = :id")
	Optional<AiRecommendationRequestEntity> findByIdWithUser(@Param("id") UUID id);
}
