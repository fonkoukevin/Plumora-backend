package com.plumora.api.book.infrastructure.gutendex;

import com.fasterxml.jackson.annotation.JsonProperty;

public record GutendexAuthorResponse(
	String name,
	@JsonProperty("birth_year")
	Integer birthYear,
	@JsonProperty("death_year")
	Integer deathYear
) {
}
