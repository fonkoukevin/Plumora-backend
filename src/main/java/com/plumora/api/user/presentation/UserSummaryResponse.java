package com.plumora.api.user.presentation;

import java.util.UUID;

public record UserSummaryResponse(
	UUID id,
	String username
) {
}
