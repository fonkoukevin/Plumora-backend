package com.plumora.api.reading.presentation;

import java.util.UUID;

public record ReadChapterResponse(
	UUID id,
	String title,
	String content,
	int chapterOrder,
	int wordCount
) {
}
