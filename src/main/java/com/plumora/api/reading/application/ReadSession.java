package com.plumora.api.reading.application;

import com.plumora.api.book.domain.Book;
import com.plumora.api.book.domain.Chapter;
import com.plumora.api.reading.domain.ReadingProgress;
import java.util.List;

public record ReadSession(
	Book book,
	List<Chapter> chapters,
	ReadingProgress progress
) {
}
