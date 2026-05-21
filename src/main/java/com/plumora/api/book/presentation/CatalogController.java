package com.plumora.api.book.presentation;

import com.plumora.api.book.application.CatalogService;
import com.plumora.api.book.domain.Book;
import com.plumora.api.shared.presentation.PageResponse;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/catalog")
public class CatalogController {

	private final CatalogService catalogService;

	public CatalogController(CatalogService catalogService) {
		this.catalogService = catalogService;
	}

	@GetMapping("/books")
	public PageResponse<CatalogBookResponse> getBooks(
		@RequestParam(defaultValue = "0") int page,
		@RequestParam(defaultValue = "20") int size
	) {
		return PageResponse.from(catalogService.getBooks(page, size), BookMapper::toCatalogResponse);
	}

	@GetMapping("/books/{bookId}")
	public CatalogBookDetailResponse getBook(@PathVariable UUID bookId) {
		Book book = catalogService.getBook(bookId);
		return BookMapper.toCatalogDetailResponse(book, catalogService.countChapters(book));
	}

	@GetMapping("/books/search")
	public PageResponse<CatalogBookResponse> searchBooks(
		@RequestParam(required = false) String q,
		@RequestParam(required = false) String genre,
		@RequestParam(defaultValue = "0") int page,
		@RequestParam(defaultValue = "20") int size
	) {
		return PageResponse.from(catalogService.searchBooks(q, genre, page, size), BookMapper::toCatalogResponse);
	}

	@GetMapping("/books/popular")
	public PageResponse<CatalogBookResponse> getPopularBooks(
		@RequestParam(defaultValue = "0") int page,
		@RequestParam(defaultValue = "20") int size
	) {
		return PageResponse.from(catalogService.getPopularBooks(page, size), BookMapper::toCatalogResponse);
	}

	@GetMapping("/books/latest")
	public PageResponse<CatalogBookResponse> getLatestBooks(
		@RequestParam(defaultValue = "0") int page,
		@RequestParam(defaultValue = "20") int size
	) {
		return PageResponse.from(catalogService.getLatestBooks(page, size), BookMapper::toCatalogResponse);
	}

	@GetMapping("/genres")
	public List<String> getGenres() {
		return catalogService.getGenres();
	}
}
