package com.plumora.api.betaReading.presentation;

import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;

public record UpdateSharedChaptersRequest(
	@NotNull List<@NotNull UUID> chapterIds
) {
}
