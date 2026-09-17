package com.plumora.api.admin.presentation;

/**
 * Summary of one bulk-import call (see AdminController#bulkImportGutendexBooks). Bounded and
 * synchronous by design - see AdminService#bulkImportGutendexBooks for why - so a caller wanting
 * more than one page's worth of new books repeats the call with {@code nextPage} until
 * {@code hasMore} is false or {@code imported} reaches the count they want.
 */
public record AdminBulkImportGutendexResponse(
	int scanned,
	int imported,
	int alreadyExisted,
	int failed,
	int nextPage,
	boolean hasMore
) {
}
