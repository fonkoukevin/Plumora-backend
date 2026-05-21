package com.plumora.api.betaReading.presentation;

import com.plumora.api.betaReading.domain.BetaCommentFeedbackType;
import com.plumora.api.betaReading.domain.BetaCommentPriority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record CreateBetaCommentRequest(
	@NotNull UUID campaignId,
	@NotNull UUID chapterId,
	@NotBlank @Size(max = 5000) String commentText,
	@Size(max = 2000) String selectedText,
	@PositiveOrZero Integer positionStart,
	@PositiveOrZero Integer positionEnd,
	@NotNull BetaCommentFeedbackType feedbackType,
	@NotNull BetaCommentPriority priority
) {
}
