package com.plumora.api.book.presentation;

import com.plumora.api.book.application.ExternalBookService;
import com.plumora.api.book.application.ImportedExternalBookResult;
import com.plumora.api.shared.presentation.PageResponse;
import java.security.Principal;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ExternalBookController {

	private final ExternalBookService externalBookService;

	public ExternalBookController(ExternalBookService externalBookService) {
		this.externalBookService = externalBookService;
	}

	@GetMapping("/external-books")
	public PageResponse<ExternalBookDto> searchExternalBooks(
		@RequestParam(required = false) String search,
		@RequestParam(required = false) String language,
		@RequestParam(required = false) String topic,
		@RequestParam(required = false) String genre,
		@RequestParam(defaultValue = "0") int page
	) {
		return PageResponse.from(
			externalBookService.searchExternalBooks(search, language, topicOrGenre(topic, genre), page),
			ExternalBookMapper::toResponse
		);
	}

	@GetMapping("/external-books/filters")
	public List<ExternalBookFilterResponse> getDiscoverFilters() {
		return externalBookService.getDiscoverFilters()
			.stream()
			.map(filter -> new ExternalBookFilterResponse(filter.label(), filter.topic()))
			.toList();
	}

	@GetMapping("/external-books/{gutendexId}")
	public ExternalBookDto getExternalBook(@PathVariable int gutendexId) {
		return ExternalBookMapper.toResponse(externalBookService.getExternalBook(gutendexId));
	}

	@PostMapping("/books/import/gutendex/{gutendexId}")
	@PreAuthorize("isAuthenticated()")
	public ResponseEntity<BookResponse> importGutendexBook(
		Principal principal,
		@PathVariable int gutendexId
	) {
		ImportedExternalBookResult result = externalBookService.importGutendexBook(principal.getName(), gutendexId);
		HttpStatus status = result.created() ? HttpStatus.CREATED : HttpStatus.OK;
		return ResponseEntity.status(status).body(BookMapper.toResponse(result.book()));
	}

	private String topicOrGenre(String topic, String genre) {
		return org.springframework.util.StringUtils.hasText(topic) ? topic : genre;
	}
}
