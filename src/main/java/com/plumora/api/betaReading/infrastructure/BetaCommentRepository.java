package com.plumora.api.betaReading.infrastructure;

import com.plumora.api.betaReading.domain.BetaComment;
import com.plumora.api.betaReading.domain.BetaReadingCampaign;
import com.plumora.api.book.domain.Book;
import com.plumora.api.book.domain.Chapter;
import com.plumora.api.user.domain.User;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BetaCommentRepository extends JpaRepository<BetaComment, UUID> {
	@EntityGraph(attributePaths = {"campaign", "campaign.book", "campaign.author", "chapter", "betaReader"})
	List<BetaComment> findByCampaignOrderByCreatedAtDesc(BetaReadingCampaign campaign);

	@EntityGraph(attributePaths = {"campaign", "campaign.book", "campaign.author", "chapter", "betaReader"})
	List<BetaComment> findByCampaignAndBetaReaderOrderByCreatedAtDesc(BetaReadingCampaign campaign, User betaReader);

	@EntityGraph(attributePaths = {"campaign", "campaign.book", "campaign.author", "chapter", "betaReader"})
	List<BetaComment> findByChapterOrderByCreatedAtDesc(Chapter chapter);

	@EntityGraph(attributePaths = {"campaign", "campaign.book", "campaign.author", "chapter", "betaReader"})
	List<BetaComment> findByChapterAndBetaReaderOrderByCreatedAtDesc(Chapter chapter, User betaReader);

	@EntityGraph(attributePaths = {"campaign", "campaign.book", "campaign.author", "chapter", "betaReader"})
	@Query("select c from BetaComment c where c.campaign.book = :book order by c.createdAt desc")
	List<BetaComment> findByBookOrderByCreatedAtDesc(@Param("book") Book book);

	@EntityGraph(attributePaths = {"campaign", "campaign.book", "campaign.author", "chapter", "betaReader"})
	@Query("select c from BetaComment c where c.id = :id")
	Optional<BetaComment> findByIdWithDetails(@Param("id") UUID id);

	@Query(
		"select distinct c.campaign.id from BetaComment c "
			+ "where c.betaReader.id = :betaReaderId and c.campaign.id in :campaignIds"
	)
	Set<UUID> findCommentedCampaignIds(
		@Param("betaReaderId") UUID betaReaderId,
		@Param("campaignIds") Collection<UUID> campaignIds
	);
}
