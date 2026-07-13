package com.plumora.api.betaReading.presentation;

import com.plumora.api.betaReading.domain.BetaComment;
import com.plumora.api.betaReading.domain.BetaInvitation;
import com.plumora.api.betaReading.domain.BetaReadingCampaign;
import com.plumora.api.book.domain.Chapter;
import com.plumora.api.book.presentation.BookCoverUrlMapper;

public final class BetaReadingMapper {
	private BetaReadingMapper() {
	}

	public static BetaCampaignResponse toCampaignResponse(BetaReadingCampaign campaign) {
		return toCampaignResponse(campaign, false);
	}

	public static BetaCampaignResponse toCampaignResponse(BetaReadingCampaign campaign, boolean engagedByMe) {
		return new BetaCampaignResponse(
			campaign.getId(),
			campaign.getBook().getId(),
			campaign.getBook().getTitle(),
			BookCoverUrlMapper.toResponseCoverUrl(campaign.getBook().getCoverUrl()),
			campaign.getAuthor().getId(),
			campaign.getAuthor().getUsername(),
			campaign.getTitle(),
			campaign.getInstructions(),
			campaign.getDeadline(),
			campaign.getStatus(),
			campaign.getCreatedAt(),
			campaign.getClosedAt(),
			engagedByMe
		);
	}

	public static BetaInvitationResponse toInvitationResponse(BetaInvitation invitation) {
		return new BetaInvitationResponse(
			invitation.getId(),
			invitation.getCampaign().getId(),
			invitation.getCampaign().getTitle(),
			invitation.getCampaign().getBook().getId(),
			invitation.getCampaign().getBook().getTitle(),
			BookCoverUrlMapper.toResponseCoverUrl(invitation.getCampaign().getBook().getCoverUrl()),
			invitation.getBetaReader().getId(),
			invitation.getBetaReader().getUsername(),
			invitation.getStatus(),
			invitation.getInvitedAt(),
			invitation.getRespondedAt()
		);
	}

	public static BetaSharedChapterResponse toSharedChapterResponse(Chapter chapter) {
		return new BetaSharedChapterResponse(
			chapter.getId(),
			chapter.getTitle(),
			chapter.getContent(),
			chapter.getChapterOrder(),
			chapter.getWordCount()
		);
	}

	public static BetaCommentResponse toCommentResponse(BetaComment comment) {
		return new BetaCommentResponse(
			comment.getId(),
			comment.getCampaign().getId(),
			comment.getCampaign().getTitle(),
			comment.getCampaign().getBook().getId(),
			comment.getCampaign().getBook().getTitle(),
			BookCoverUrlMapper.toResponseCoverUrl(comment.getCampaign().getBook().getCoverUrl()),
			comment.getChapter().getId(),
			comment.getChapter().getTitle(),
			comment.getBetaReader().getId(),
			comment.getBetaReader().getUsername(),
			comment.getCommentText(),
			comment.getSelectedText(),
			comment.getPositionStart(),
			comment.getPositionEnd(),
			comment.getFeedbackType(),
			comment.getPriority(),
			comment.getStatus(),
			comment.getCreatedAt(),
			comment.getUpdatedAt()
		);
	}
}
