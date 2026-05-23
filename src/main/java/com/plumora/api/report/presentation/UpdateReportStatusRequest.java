package com.plumora.api.report.presentation;

import com.plumora.api.report.domain.ReportStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateReportStatusRequest(
	@NotNull
	ReportStatus status
) {
}
