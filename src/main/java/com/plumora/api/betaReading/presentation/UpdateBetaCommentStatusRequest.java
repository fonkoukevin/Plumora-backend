package com.plumora.api.betaReading.presentation;

import com.plumora.api.betaReading.domain.BetaCommentStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateBetaCommentStatusRequest(
	@NotNull BetaCommentStatus status
) {
}
