package com.plumora.api.book.presentation;

public record RestoreChapterVersionResponse(
	ChapterResponse chapter,
	ChapterVersionResponse restoredVersion,
	ChapterVersionResponse backupVersion
) {
}
