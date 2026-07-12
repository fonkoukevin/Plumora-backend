package com.plumora.api.book.application;

public record ChapterStats(long chapterCount, long wordCount) {
	public static final ChapterStats EMPTY = new ChapterStats(0, 0);
}
