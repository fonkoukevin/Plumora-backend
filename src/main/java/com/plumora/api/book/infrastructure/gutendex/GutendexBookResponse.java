package com.plumora.api.book.infrastructure.gutendex;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;

public record GutendexBookResponse(
	int id,
	String title,
	List<GutendexAuthorResponse> authors,
	List<String> summaries,
	List<String> subjects,
	List<String> bookshelves,
	List<String> languages,
	Boolean copyright,
	@JsonProperty("media_type")
	String mediaType,
	Map<String, String> formats,
	@JsonProperty("download_count")
	int downloadCount
) {
}
