package com.plumora.api.betaReading.infrastructure;

import com.plumora.api.betaReading.domain.BetaChapterView;
import com.plumora.api.book.domain.Chapter;
import com.plumora.api.user.domain.User;
import java.util.Collection;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BetaChapterViewRepository extends JpaRepository<BetaChapterView, UUID> {

	boolean existsByChapterAndBetaReader(Chapter chapter, User betaReader);

	@Query(
		"select distinct v.campaign.id from BetaChapterView v "
			+ "where v.betaReader.id = :betaReaderId and v.campaign.id in :campaignIds"
	)
	Set<UUID> findViewedCampaignIds(
		@Param("betaReaderId") UUID betaReaderId,
		@Param("campaignIds") Collection<UUID> campaignIds
	);
}
