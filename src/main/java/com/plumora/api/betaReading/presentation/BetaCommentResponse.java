package com.plumora.api.betaReading.presentation;

import com.plumora.api.betaReading.domain.BetaCommentFeedbackType;
import com.plumora.api.betaReading.domain.BetaCommentPriority;
import com.plumora.api.betaReading.domain.BetaCommentStatus;
import java.time.LocalDateTime;
import java.util.UUID;

public record BetaCommentResponse(
	UUID id,
	UUID campaignId,
	String campaignTitle,
	UUID bookId,
	String bookTitle,
	UUID chapterId,
	String chapterTitle,
	UUID betaReaderId,
	String betaReaderUsername,
	String commentText,
	String selectedText,
	Integer positionStart,
	Integer positionEnd,
	BetaCommentFeedbackType feedbackType,
	BetaCommentPriority priority,
	BetaCommentStatus status,
	LocalDateTime createdAt,
	LocalDateTime updatedAt
) {
}
