package com.plumora.api.book.presentation;

import com.plumora.api.book.application.ExternalBook;

public final class ExternalBookMapper {
	private ExternalBookMapper() {
	}

	public static ExternalBookDto toResponse(ExternalBook book) {
		return new ExternalBookDto(
			book.externalId(),
			book.source(),
			book.title(),
			book.authors(),
			book.summary(),
			book.subjects(),
			book.languages(),
			book.copyright(),
			book.mediaType(),
			book.downloadCount(),
			book.coverUrl(),
			book.readUrl(),
			book.formats(),
			book.sourceUrl(),
			book.imported(),
			book.internalBookId()
		);
	}
}
