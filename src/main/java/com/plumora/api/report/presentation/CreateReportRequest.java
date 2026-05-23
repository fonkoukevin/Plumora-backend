package com.plumora.api.report.presentation;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateReportRequest(
	@NotBlank
	@Size(max = 100)
	String reason,

	@Size(max = 2000)
	String description
) {
}
