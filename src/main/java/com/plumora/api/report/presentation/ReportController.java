package com.plumora.api.report.presentation;

import com.plumora.api.report.application.ReportService;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping
public class ReportController {

	private final ReportService reportService;

	public ReportController(ReportService reportService) {
		this.reportService = reportService;
	}

	@PostMapping("/books/{bookId}/reports")
	@ResponseStatus(HttpStatus.CREATED)
	public ReportResponse createReport(
		Principal principal,
		@PathVariable UUID bookId,
		@Valid @RequestBody CreateReportRequest request
	) {
		return ReportMapper.toResponse(reportService.createReport(principal.getName(), bookId, request));
	}

	@GetMapping("/reports/my")
	public List<ReportResponse> getMyReports(Principal principal) {
		return reportService.getMyReports(principal.getName())
			.stream()
			.map(ReportMapper::toResponse)
			.toList();
	}

	@GetMapping("/reports")
	@PreAuthorize("hasRole('ADMIN')")
	public List<ReportResponse> getAllReports() {
		return reportService.getAllReports()
			.stream()
			.map(ReportMapper::toResponse)
			.toList();
	}

	@PatchMapping("/reports/{reportId}/status")
	@PreAuthorize("hasRole('ADMIN')")
	public ReportResponse updateReportStatus(
		@PathVariable UUID reportId,
		@Valid @RequestBody UpdateReportStatusRequest request
	) {
		return ReportMapper.toResponse(reportService.updateStatus(reportId, request));
	}
}
