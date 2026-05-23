package com.plumora.api.report.presentation;

import com.plumora.api.report.domain.ReportStatus;
import java.time.LocalDateTime;
import java.util.UUID;

public record ReportResponse(
	UUID id,
	UUID reporterId,
	String reporterUsername,
	UUID bookId,
	String bookTitle,
	String reason,
	String description,
	ReportStatus status,
	LocalDateTime createdAt,
	LocalDateTime resolvedAt
) {
}
