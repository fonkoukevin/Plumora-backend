package com.plumora.api.book.presentation;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.web.multipart.MultipartFile;

public record BookMultipartRequest(
	String title,
	String subtitle,
	String summary,
	String genre,
	String languageCode,
	@Schema(type = "string", format = "binary")
	MultipartFile coverImage
) {
}
