package com.plumora.api.reading.infrastructure;

import com.plumora.api.book.domain.ExternalBookSource;
import com.plumora.api.reading.domain.ExternalBookReview;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExternalBookReviewRepository extends JpaRepository<ExternalBookReview, UUID> {
	@EntityGraph(attributePaths = "user")
	List<ExternalBookReview> findByExternalSourceAndExternalIdOrderByCreatedAtDesc(
		ExternalBookSource externalSource,
		String externalId
	);
}
