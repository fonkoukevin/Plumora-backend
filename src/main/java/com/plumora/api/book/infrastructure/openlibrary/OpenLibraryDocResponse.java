package com.plumora.api.book.infrastructure.openlibrary;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record OpenLibraryDocResponse(
	String key,
	String title,
	@JsonProperty("author_name")
	List<String> authorName,
	@JsonProperty("cover_i")
	Integer coverId,
	List<String> isbn,
	List<String> subject
) {
}
