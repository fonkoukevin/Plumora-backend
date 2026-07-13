package com.plumora.api.admin.presentation;

import com.plumora.api.admin.application.AdminBookDetail;
import com.plumora.api.admin.domain.AdminBookType;
import com.plumora.api.book.domain.Book;
import com.plumora.api.book.presentation.BookCoverUrlMapper;
import java.util.List;

public final class AdminBookMapper {
	private AdminBookMapper() {
	}

	public static AdminBookListDto toListDto(Book book, long reportsCount) {
		return new AdminBookListDto(
			book.getId(),
			book.getTitle(),
			authors(book),
			type(book),
			book.getStatus(),
			BookCoverUrlMapper.toResponseCoverUrl(book.getCoverUrl()),
			book.getCreatedAt(),
			sourceName(book),
			book.getExternalId(),
			reportsCount
		);
	}

	public static AdminBookDetailDto toDetailDto(AdminBookDetail detail) {
		Book book = detail.book();
		return new AdminBookDetailDto(
			book.getId(),
			book.getTitle(),
			authors(book),
			book.getSummary(),
			type(book),
			book.getStatus(),
			BookCoverUrlMapper.toResponseCoverUrl(book.getCoverUrl()),
			book.getReadUrl(),
			sourceName(book),
			book.getExternalId(),
			book.getCreatedAt(),
			book.getUpdatedAt(),
			detail.reportsCount(),
			detail.chaptersCount()
		);
	}

	private static AdminBookType type(Book book) {
		return book.getExternalSource() == null ? AdminBookType.PLUMORA_WORK : AdminBookType.PUBLIC_DOMAIN;
	}

	private static String sourceName(Book book) {
		return book.getExternalSource() == null ? null : book.getExternalSource().name();
	}

	private static List<String> authors(Book book) {
		if (book.getExternalAuthors() != null && !book.getExternalAuthors().isEmpty()) {
			return book.getExternalAuthors();
		}
		return book.getAuthor() == null ? List.of() : List.of(book.getAuthor().getUsername());
	}
}
