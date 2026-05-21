package com.plumora.api.reading.presentation;

import com.plumora.api.reading.application.ReadingService;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
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
@RequestMapping
public class ReadingController {

	private final ReadingService readingService;

	public ReadingController(ReadingService readingService) {
		this.readingService = readingService;
	}

	@GetMapping("/books/{bookId}/read")
	@PreAuthorize("hasRole('READER')")
	public ReadBookResponse readBook(Principal principal, @PathVariable UUID bookId) {
		return ReadingMapper.toReadBookResponse(readingService.readBook(principal.getName(), bookId));
	}

	@GetMapping("/reading-progress/my")
	@PreAuthorize("hasRole('READER')")
	public List<ReadingProgressResponse> getMyProgress(Principal principal) {
		return readingService.getMyProgress(principal.getName())
			.stream()
			.map(ReadingMapper::toProgressResponse)
			.toList();
	}

	@GetMapping("/books/{bookId}/reading-progress")
	@PreAuthorize("hasRole('READER')")
	public ReadingProgressResponse getBookProgress(Principal principal, @PathVariable UUID bookId) {
		return ReadingMapper.toProgressResponse(readingService.getBookProgress(principal.getName(), bookId));
	}

	@PostMapping("/books/{bookId}/reading-progress")
	@ResponseStatus(HttpStatus.CREATED)
	@PreAuthorize("hasRole('READER')")
	public ReadingProgressResponse createProgress(
		Principal principal,
		@PathVariable UUID bookId,
		@Valid @RequestBody ReadingProgressRequest request
	) {
		return ReadingMapper.toProgressResponse(readingService.createProgress(principal.getName(), bookId, request));
	}

	@PutMapping("/books/{bookId}/reading-progress")
	@PreAuthorize("hasRole('READER')")
	public ReadingProgressResponse updateProgress(
		Principal principal,
		@PathVariable UUID bookId,
		@Valid @RequestBody ReadingProgressRequest request
	) {
		return ReadingMapper.toProgressResponse(readingService.updateProgress(principal.getName(), bookId, request));
	}

	@PatchMapping("/books/{bookId}/reading-progress/finish")
	@PreAuthorize("hasRole('READER')")
	public ReadingProgressResponse finishProgress(Principal principal, @PathVariable UUID bookId) {
		return ReadingMapper.toProgressResponse(readingService.finishProgress(principal.getName(), bookId));
	}
}
