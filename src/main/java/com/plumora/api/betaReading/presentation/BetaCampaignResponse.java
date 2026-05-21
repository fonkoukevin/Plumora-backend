package com.plumora.api.betaReading.presentation;

import com.plumora.api.betaReading.domain.BetaCampaignStatus;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record BetaCampaignResponse(
	UUID id,
	UUID bookId,
	String bookTitle,
	UUID authorId,
	String authorUsername,
	String title,
	String instructions,
	LocalDate deadline,
	BetaCampaignStatus status,
	LocalDateTime createdAt,
	LocalDateTime closedAt
) {
}
