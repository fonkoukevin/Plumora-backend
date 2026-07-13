package com.plumora.api.admin.presentation;

import jakarta.validation.constraints.Size;

public record AdminReportActionRequest(
	@Size(max = 500) String reason
) {
}
