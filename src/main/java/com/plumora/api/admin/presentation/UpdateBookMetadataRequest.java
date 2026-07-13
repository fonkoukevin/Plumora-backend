package com.plumora.api.admin.presentation;

import jakarta.validation.constraints.Size;
import java.util.List;

public record UpdateBookMetadataRequest(
	@Size(max = 150) String title,
	List<String> authors,
	@Size(max = 5000) String summary,
	List<String> subjects,
	List<String> languages,
	@Size(max = 500) String coverUrl
) {
}
