package com.plumora.api.book.infrastructure.openlibrary;

import java.util.List;

record OpenLibrarySearchResponse(
	List<OpenLibraryDocResponse> docs
) {
}
