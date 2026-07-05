package com.plumora.api.book.application;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public record ExternalBook(
	String externalId,
	String source,
	String title,
	List<String> authors,
	String summary,
	List<String> subjects,
	List<String> languages,
	Boolean copyright,
	String mediaType,
	int downloadCount,
	String coverUrl,
	String readUrl,
	Map<String, String> formats,
	String sourceUrl,
	boolean imported,
	UUID internalBookId
) {
}
