package com.plumora.api.reading.presentation;

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

	public ReviewController(ReviewService reviewService) {
		this.reviewService = reviewService;
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
