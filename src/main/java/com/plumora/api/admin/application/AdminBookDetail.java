package com.plumora.api.admin.application;

import com.plumora.api.book.domain.Book;

public record AdminBookDetail(Book book, long reportsCount, long chaptersCount) {
}
