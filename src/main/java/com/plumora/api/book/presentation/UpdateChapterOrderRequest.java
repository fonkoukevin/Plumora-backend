package com.plumora.api.book.presentation;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record UpdateChapterOrderRequest(
	@NotNull @Min(1) Integer chapterOrder
) {
}
