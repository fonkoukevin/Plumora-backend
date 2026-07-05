package com.plumora.api.report.presentation;

import com.plumora.api.book.presentation.BookCoverUrlMapper;
import com.plumora.api.report.domain.Report;

public final class ReportMapper {
	private ReportMapper() {
	}

	public static ReportResponse toResponse(Report report) {
		return new ReportResponse(
			report.getId(),
			report.getReporter().getId(),
			report.getReporter().getUsername(),
			report.getBook().getId(),
			report.getBook().getTitle(),
			BookCoverUrlMapper.toResponseCoverUrl(report.getBook().getCoverUrl()),
			report.getReason(),
			report.getDescription(),
			report.getStatus(),
			report.getCreatedAt(),
			report.getResolvedAt()
		);
	}
}
