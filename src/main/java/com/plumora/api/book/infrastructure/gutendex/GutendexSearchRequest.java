package com.plumora.api.book.infrastructure.gutendex;

import org.springframework.util.StringUtils;

public record GutendexSearchRequest(
	String search,
	String language,
	String topic,
	int page,
	String sort,
	boolean copyright
) {
	public GutendexSearchRequest {
		page = Math.max(page, 1);
		sort = StringUtils.hasText(sort) ? sort.trim() : "popular";
	}

	public static GutendexSearchRequest publicDomain(String search, String language, String topic, int page) {
		return new GutendexSearchRequest(search, language, topic, page, "popular", false);
	}
}
