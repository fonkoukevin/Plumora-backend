package com.plumora.api.betaReading.infrastructure;

import com.plumora.api.betaReading.domain.BetaInvitation;
import com.plumora.api.betaReading.domain.BetaReadingCampaign;
import com.plumora.api.user.domain.User;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BetaInvitationRepository extends JpaRepository<BetaInvitation, UUID> {
	@EntityGraph(attributePaths = {"campaign", "campaign.book", "campaign.author", "betaReader"})
	List<BetaInvitation> findByCampaignOrderByInvitedAtDesc(BetaReadingCampaign campaign);

	@EntityGraph(attributePaths = {"campaign", "campaign.book", "campaign.author", "betaReader"})
	List<BetaInvitation> findByBetaReaderOrderByInvitedAtDesc(User betaReader);

	@EntityGraph(attributePaths = {"campaign", "campaign.book", "campaign.author", "betaReader"})
	@Query("select i from BetaInvitation i where i.id = :id")
	Optional<BetaInvitation> findByIdWithCampaignAndReader(@Param("id") UUID id);

	boolean existsByCampaignAndBetaReader(BetaReadingCampaign campaign, User betaReader);
}
