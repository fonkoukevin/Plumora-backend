package com.plumora.api.betaReading.infrastructure;

import com.plumora.api.betaReading.domain.BetaReadingCampaign;
import com.plumora.api.betaReading.domain.BetaSharedChapter;
import com.plumora.api.book.domain.Chapter;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BetaSharedChapterRepository extends JpaRepository<BetaSharedChapter, UUID> {
	@EntityGraph(attributePaths = {"campaign", "chapter", "chapter.book"})
	List<BetaSharedChapter> findByCampaignOrderByChapterChapterOrderAsc(BetaReadingCampaign campaign);

	boolean existsByCampaignAndChapter(BetaReadingCampaign campaign, Chapter chapter);

	void deleteByCampaign(BetaReadingCampaign campaign);
}
