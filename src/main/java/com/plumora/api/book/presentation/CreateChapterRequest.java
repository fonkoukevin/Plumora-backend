package com.plumora.api.book.presentation;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateChapterRequest(
	@NotBlank @Size(max = 150) String title,
	@Size(max = 100000) String content,
	@NotNull @Min(1) Integer chapterOrder
) {
}
