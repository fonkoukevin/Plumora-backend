package com.plumora.api.book.presentation;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateChapterRequest(
	@NotBlank @Size(max = 150) String title,
	@Size(max = 100000) String content
) {
}
