package com.plumora.api.betaReading.presentation;

import java.util.UUID;

public record BetaSharedChapterResponse(
	UUID id,
	String title,
	String content,
	int chapterOrder,
	int wordCount
) {
}
