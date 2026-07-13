package com.plumora.api.admin.presentation;

import java.util.List;

public record AdminDashboardDto(
	long totalUsers,
	long activeUsers,
	long totalBooks,
	long plumoraBooks,
	long publicDomainBooks,
	long pendingReports,
	long resolvedReports,
	long archivedBooks,
	long aiCallsCount,
	List<AdminActionLogDto> recentAdminActions
) {
}
