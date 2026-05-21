package com.plumora.api.betaReading.presentation;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record CreateBetaInvitationRequest(
	@NotNull UUID betaReaderId
) {
}
