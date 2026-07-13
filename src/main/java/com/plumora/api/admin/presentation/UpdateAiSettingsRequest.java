package com.plumora.api.admin.presentation;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateAiSettingsRequest(
	@NotNull Boolean enabled,
	@Size(max = 500) String reason
) {
}
