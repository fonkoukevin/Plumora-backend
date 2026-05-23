package com.plumora.api.book.presentation;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateBookRequest(
	@NotBlank @Size(max = 150) String title,
	@Size(max = 200) String subtitle,
	@Size(max = 5000) String summary,
	@JsonAlias({"cover_url", "coverImageUrl", "cover_image_url", "imageUrl", "image_url", "bookCoverUrl", "book_cover_url", "image"})
	@Size(max = 500) String coverUrl,
	@NotBlank @Size(max = 80) String genre,
	@Size(max = 10) String languageCode
) {
}
