package com.plumora.api.betaReading.presentation;

import com.plumora.api.betaReading.domain.BetaInvitationStatus;
import java.time.LocalDateTime;
import java.util.UUID;

public record BetaInvitationResponse(
	UUID id,
	UUID campaignId,
	String campaignTitle,
	UUID bookId,
	String bookTitle,
	UUID betaReaderId,
	String betaReaderUsername,
	BetaInvitationStatus status,
	LocalDateTime invitedAt,
	LocalDateTime respondedAt
) {
}
