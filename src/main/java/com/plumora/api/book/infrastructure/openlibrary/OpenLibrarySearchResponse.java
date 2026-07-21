package com.plumora.api.book.infrastructure.openlibrary;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record OpenLibrarySearchResponse(
	@JsonProperty("numFound")
	int numFound,
	List<OpenLibraryDocResponse> docs
) {
}
