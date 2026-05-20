package com.plumora.api.book.presentation;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateBookRequest(
	@NotBlank @Size(max = 150) String title,
	@Size(max = 200) String subtitle,
	@Size(max = 5000) String summary,
	@Size(max = 500) String coverUrl,
	@NotBlank @Size(max = 80) String genre,
	@Size(max = 10) String languageCode
) {
}
