package com.plumora.api.book.infrastructure;

import java.util.UUID;

public interface BookChapterStats {
	UUID getBookId();

	long getChapterCount();

	long getWordCount();
}
