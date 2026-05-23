package com.plumora.api.book.presentation;

import com.plumora.api.book.application.BookCoverStorage;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/uploads/book-covers")
public class BookCoverController {
	private final BookCoverStorage bookCoverStorage;

	public BookCoverController(BookCoverStorage bookCoverStorage) {
		this.bookCoverStorage = bookCoverStorage;
	}

	@GetMapping("/{filename:.+}")
	public ResponseEntity<Resource> getBookCover(@PathVariable String filename) {
		Resource resource = bookCoverStorage.load(filename);
		return ResponseEntity.ok()
			.cacheControl(CacheControl.noCache())
			.contentType(bookCoverStorage.mediaType(filename))
			.body(resource);
	}
}
