package com.plumora.api.book.application;

import com.plumora.api.book.domain.Book;

public record ImportedExternalBookResult(
	Book book,
	boolean created
) {
}
