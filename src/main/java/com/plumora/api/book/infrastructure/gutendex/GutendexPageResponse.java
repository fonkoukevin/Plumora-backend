package com.plumora.api.book.infrastructure.gutendex;

import java.util.List;

public record GutendexPageResponse(
	int count,
	String next,
	String previous,
	List<GutendexBookResponse> results
) {
	public static GutendexPageResponse empty() {
		return new GutendexPageResponse(0, null, null, List.of());
	}
}
