package com.plumora.api.reading.presentation;

import com.plumora.api.reading.application.ExternalBookReviewService;
import com.plumora.api.reading.application.ReviewService;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping
public class ReviewController {

	private final ReviewService reviewService;
	private final ExternalBookReviewService externalBookReviewService;

	public ReviewController(ReviewService reviewService, ExternalBookReviewService externalBookReviewService) {
		this.reviewService = reviewService;
		this.externalBookReviewService = externalBookReviewService;
	}

	@PostMapping("/books/{bookId}/reviews")
	@ResponseStatus(HttpStatus.CREATED)
	@PreAuthorize("hasRole('READER')")
	public ReviewResponse createReview(
		Principal principal,
		@PathVariable UUID bookId,
		@Valid @RequestBody ReviewRequest request
	) {
		return ReadingMapper.toReviewResponse(reviewService.createReview(principal.getName(), bookId, request));
	}

	@GetMapping("/books/{bookId}/reviews")
	@PreAuthorize("hasRole('READER')")
	public List<ReviewResponse> getBookReviews(@PathVariable UUID bookId) {
		return reviewService.getBookReviews(bookId)
			.stream()
			.map(ReadingMapper::toReviewResponse)
			.toList();
	}

	@GetMapping("/external-books/{gutendexId}/reviews")
	public List<ExternalBookReviewResponse> getExternalBookReviews(@PathVariable int gutendexId) {
		return externalBookReviewService.getGutendexReviews(gutendexId)
			.stream()
			.map(ReadingMapper::toExternalBookReviewResponse)
			.toList();
	}

	@PostMapping("/external-books/{gutendexId}/reviews")
	@ResponseStatus(HttpStatus.CREATED)
	@PreAuthorize("hasRole('READER')")
	public ExternalBookReviewResponse createExternalBookReview(
		Principal principal,
		@PathVariable int gutendexId,
		@Valid @RequestBody ReviewRequest request
	) {
		return ReadingMapper.toExternalBookReviewResponse(
			externalBookReviewService.createGutendexReview(principal.getName(), gutendexId, request)
		);
	}

	@GetMapping("/reviews/my")
	@PreAuthorize("hasRole('READER')")
	public List<ReviewResponse> getMyReviews(Principal principal) {
		return reviewService.getMyReviews(principal.getName())
			.stream()
			.map(ReadingMapper::toReviewResponse)
			.toList();
	}

	@PutMapping("/reviews/{reviewId}")
	@PreAuthorize("hasRole('READER')")
	public ReviewResponse updateReview(
		Principal principal,
		@PathVariable UUID reviewId,
		@Valid @RequestBody ReviewRequest request
	) {
		return ReadingMapper.toReviewResponse(reviewService.updateReview(principal.getName(), reviewId, request));
	}

	@DeleteMapping("/reviews/{reviewId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@PreAuthorize("hasRole('READER')")
	public void deleteReview(Principal principal, @PathVariable UUID reviewId) {
		reviewService.deleteReview(principal.getName(), reviewId);
	}
}
