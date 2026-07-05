package com.plumora.api.reading.application;

import com.plumora.api.book.domain.Book;
import com.plumora.api.book.domain.BookStatus;
import com.plumora.api.book.domain.BookVisibility;
import com.plumora.api.book.infrastructure.BookRepository;
import com.plumora.api.reading.domain.Review;
import com.plumora.api.reading.infrastructure.ReviewRepository;
import com.plumora.api.reading.presentation.ReviewRequest;
import com.plumora.api.shared.exception.BusinessException;
import com.plumora.api.shared.exception.ResourceNotFoundException;
import com.plumora.api.shared.exception.UnauthorizedActionException;
import com.plumora.api.user.application.UserService;
import com.plumora.api.user.domain.User;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReviewService {

	private final BookRepository bookRepository;
	private final ReviewRepository reviewRepository;
	private final UserService userService;

	public ReviewService(
		BookRepository bookRepository,
		ReviewRepository reviewRepository,
		UserService userService
	) {
		this.bookRepository = bookRepository;
		this.reviewRepository = reviewRepository;
		this.userService = userService;
	}

	@Transactional
	public Review createReview(String currentUserEmail, UUID bookId, ReviewRequest request) {
		User user = userService.getCurrentUser(currentUserEmail);
		Book book = getPublishedPublicBook(bookId);

		Review review = new Review();
		review.setUser(user);
		review.setBook(book);
		applyReviewFields(review, request);
		Review savedReview = reviewRepository.save(review);
		recalculateAverageRating(book);
		return savedReview;
	}

	@Transactional(readOnly = true)
	public List<Review> getBookReviews(UUID bookId) {
		Book book = getPublishedPublicBook(bookId);
		return reviewRepository.findByBookOrderByCreatedAtDesc(book);
	}

	@Transactional(readOnly = true)
	public List<Review> getMyReviews(String currentUserEmail) {
		User user = userService.getCurrentUser(currentUserEmail);
		return reviewRepository.findByUserOrderByCreatedAtDesc(user);
	}

	@Transactional
	public Review updateReview(String currentUserEmail, UUID reviewId, ReviewRequest request) {
		Review review = getReview(reviewId);
		ensureOwner(currentUserEmail, review);
		ensurePublishedPublic(review.getBook(), "Only published public books can receive reviews");
		applyReviewFields(review, request);
		Review savedReview = reviewRepository.save(review);
		recalculateAverageRating(review.getBook());
		return savedReview;
	}

	@Transactional
	public void deleteReview(String currentUserEmail, UUID reviewId) {
		Review review = getReview(reviewId);
		ensureOwner(currentUserEmail, review);
		Book book = review.getBook();
		reviewRepository.delete(review);
		recalculateAverageRating(book);
	}

	private Review getReview(UUID reviewId) {
		return reviewRepository.findById(reviewId)
			.orElseThrow(() -> new ResourceNotFoundException("Review was not found"));
	}

	private void ensureOwner(String currentUserEmail, Review review) {
		if (!review.getUser().getEmail().equals(currentUserEmail)) {
			throw new UnauthorizedActionException("Only the review author can manage this review");
		}
	}

	private void applyReviewFields(Review review, ReviewRequest request) {
		if (request.rating() == null || request.rating() < 1 || request.rating() > 5) {
			throw new BusinessException("Rating must be between 1 and 5");
		}
		review.setRating(request.rating());
		review.setComment(request.comment());
	}

	private void recalculateAverageRating(Book book) {
		List<Review> reviews = reviewRepository.findByBook(book);
		if (reviews.isEmpty()) {
			book.setAverageRating(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
		} else {
			double average = reviews.stream()
				.mapToInt(Review::getRating)
				.average()
				.orElse(0);
			book.setAverageRating(BigDecimal.valueOf(average).setScale(2, RoundingMode.HALF_UP));
		}
		bookRepository.save(book);
	}

	private Book getPublishedPublicBook(UUID bookId) {
		Book book = bookRepository.findByIdWithAuthor(bookId)
			.orElseThrow(() -> new ResourceNotFoundException("Book was not found"));
		ensurePublishedPublic(book, "Only published public books can receive reviews");
		return book;
	}

	private void ensurePublishedPublic(Book book, String message) {
		if (book.getStatus() != BookStatus.PUBLISHED
			|| book.getVisibility() != BookVisibility.PUBLIC
			|| book.getPublishedAt() == null) {
			throw new BusinessException(message);
		}
	}
}
