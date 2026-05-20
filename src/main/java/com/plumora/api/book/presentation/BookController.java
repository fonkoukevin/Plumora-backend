package com.plumora.api.book.presentation;

import com.plumora.api.book.application.BookService;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/books")
public class BookController {

	private final BookService bookService;

	public BookController(BookService bookService) {
		this.bookService = bookService;
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@PreAuthorize("hasRole('AUTHOR')")
	public BookResponse createBook(
		Principal principal,
		@Valid @RequestBody CreateBookRequest request
	) {
		return BookMapper.toResponse(bookService.createBook(principal.getName(), request));
	}

	@GetMapping("/my-books")
	@PreAuthorize("hasRole('AUTHOR')")
	public List<BookResponse> getMyBooks(Principal principal) {
		return bookService.getMyBooks(principal.getName())
			.stream()
			.map(BookMapper::toResponse)
			.toList();
	}

	@GetMapping("/{bookId}")
	public BookResponse getBook(@PathVariable UUID bookId) {
		return BookMapper.toResponse(bookService.getBook(bookId));
	}

	@PutMapping("/{bookId}")
	@PreAuthorize("hasRole('AUTHOR')")
	public BookResponse updateBook(
		Principal principal,
		@PathVariable UUID bookId,
		@Valid @RequestBody UpdateBookRequest request
	) {
		return BookMapper.toResponse(bookService.updateBook(principal.getName(), bookId, request));
	}

	@DeleteMapping("/{bookId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@PreAuthorize("hasRole('AUTHOR')")
	public void deleteBook(Principal principal, @PathVariable UUID bookId) {
		bookService.deleteBook(principal.getName(), bookId);
	}

	@PatchMapping("/{bookId}/ready")
	@PreAuthorize("hasRole('AUTHOR')")
	public BookResponse markReady(Principal principal, @PathVariable UUID bookId) {
		return BookMapper.toResponse(bookService.markReady(principal.getName(), bookId));
	}

	@PatchMapping("/{bookId}/publish")
	@PreAuthorize("hasRole('AUTHOR')")
	public BookResponse publishBook(Principal principal, @PathVariable UUID bookId) {
		return BookMapper.toResponse(bookService.publishBook(principal.getName(), bookId));
	}

	@PatchMapping("/{bookId}/archive")
	@PreAuthorize("hasRole('AUTHOR')")
	public BookResponse archiveBook(Principal principal, @PathVariable UUID bookId) {
		return BookMapper.toResponse(bookService.archiveBook(principal.getName(), bookId));
	}
}
