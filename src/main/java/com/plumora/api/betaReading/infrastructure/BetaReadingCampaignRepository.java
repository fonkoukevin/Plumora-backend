package com.plumora.api.betaReading.infrastructure;

import com.plumora.api.betaReading.domain.BetaCampaignStatus;
import com.plumora.api.betaReading.domain.BetaReadingCampaign;
import com.plumora.api.book.domain.Book;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BetaReadingCampaignRepository extends JpaRepository<BetaReadingCampaign, UUID> {
	@EntityGraph(attributePaths = {"book", "author"})
	List<BetaReadingCampaign> findByBookOrderByCreatedAtDesc(Book book);

	@EntityGraph(attributePaths = {"book", "author"})
	List<BetaReadingCampaign> findByStatusOrderByCreatedAtDesc(BetaCampaignStatus status);

	@EntityGraph(attributePaths = {"book", "author"})
	@Query("select c from BetaReadingCampaign c where c.id = :id")
	Optional<BetaReadingCampaign> findByIdWithBookAndAuthor(@Param("id") UUID id);
}
