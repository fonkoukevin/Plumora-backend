package com.plumora.api.reading.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.plumora.api.book.domain.Book;
import com.plumora.api.book.domain.BookStatus;
import com.plumora.api.book.domain.BookVisibility;
import com.plumora.api.book.infrastructure.BookRepository;
import com.plumora.api.reading.domain.Review;
import com.plumora.api.reading.infrastructure.ReviewRepository;
import com.plumora.api.reading.presentation.ReviewRequest;
import com.plumora.api.shared.exception.BusinessException;
import com.plumora.api.shared.exception.DuplicateResourceException;
import com.plumora.api.shared.exception.UnauthorizedActionException;
import com.plumora.api.user.application.UserService;
import com.plumora.api.user.domain.User;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {

	@Mock
	private BookRepository bookRepository;

	@Mock
	private ReviewRepository reviewRepository;

	@Mock
	private UserService userService;

	private ReviewService reviewService;

	@BeforeEach
	void setUp() {
		reviewService = new ReviewService(bookRepository, reviewRepository, userService);
	}

	@Test
	void createReviewRecalculatesAverageRating() {
		User reader = user("reader@example.com");
		Book book = publishedBook(user("author@example.com"));
		Review existingReview = review(user("other@example.com"), book, 3);

		when(userService.getCurrentUser(reader.getEmail())).thenReturn(reader);
		when(bookRepository.findByIdWithAuthor(book.getId())).thenReturn(Optional.of(book));
		when(reviewRepository.existsByUserAndBook(reader, book)).thenReturn(false);
		when(reviewRepository.save(any(Review.class))).thenAnswer(invocation -> {
			Review review = invocation.getArgument(0);
			review.setId(UUID.randomUUID());
			return review;
		});
		when(reviewRepository.findByBook(book)).thenAnswer(invocation -> List.of(existingReview, review(reader, book, 5)));

		Review savedReview = reviewService.createReview(reader.getEmail(), book.getId(), new ReviewRequest(5, "Loved it"));

		assertThat(savedReview.getRating()).isEqualTo(5);
		assertThat(book.getAverageRating()).isEqualByComparingTo("4.00");
		verify(bookRepository).save(book);
	}

	@Test
	void createReviewRejectsDuplicateForSameReaderAndBook() {
		User reader = user("reader@example.com");
		Book book = publishedBook(user("author@example.com"));

		when(userService.getCurrentUser(reader.getEmail())).thenReturn(reader);
		when(bookRepository.findByIdWithAuthor(book.getId())).thenReturn(Optional.of(book));
		when(reviewRepository.existsByUserAndBook(reader, book)).thenReturn(true);

		assertThatThrownBy(() -> reviewService.createReview(reader.getEmail(), book.getId(), new ReviewRequest(5, "Great")))
			.isInstanceOf(DuplicateResourceException.class)
			.hasMessage("Review already exists for this book");
	}

	@Test
	void updateReviewRequiresReviewOwner() {
		User owner = user("owner@example.com");
		User other = user("other@example.com");
		Book book = publishedBook(user("author@example.com"));
		Review review = review(owner, book, 4);

		when(reviewRepository.findById(review.getId())).thenReturn(Optional.of(review));

		assertThatThrownBy(() -> reviewService.updateReview(other.getEmail(), review.getId(), new ReviewRequest(5, "Nope")))
			.isInstanceOf(UnauthorizedActionException.class)
			.hasMessage("Only the review author can manage this review");
	}

	@Test
	void updateReviewRejectsRatingOutsideBounds() {
		User reader = user("reader@example.com");
		Book book = publishedBook(user("author@example.com"));
		Review review = review(reader, book, 4);

		when(reviewRepository.findById(review.getId())).thenReturn(Optional.of(review));

		assertThatThrownBy(() -> reviewService.updateReview(reader.getEmail(), review.getId(), new ReviewRequest(6, "Too much")))
			.isInstanceOf(BusinessException.class)
			.hasMessage("Rating must be between 1 and 5");
	}

	@Test
	void deleteReviewResetsAverageRatingWhenNoReviewsRemain() {
		User reader = user("reader@example.com");
		Book book = publishedBook(user("author@example.com"));
		book.setAverageRating(java.math.BigDecimal.valueOf(4.50));
		Review review = review(reader, book, 5);

		when(reviewRepository.findById(review.getId())).thenReturn(Optional.of(review));
		when(reviewRepository.findByBook(book)).thenReturn(List.of());

		reviewService.deleteReview(reader.getEmail(), review.getId());

		assertThat(book.getAverageRating()).isEqualByComparingTo("0.00");
		verify(reviewRepository).delete(review);
		verify(bookRepository).save(book);
	}

	private Review review(User user, Book book, int rating) {
		Review review = new Review();
		review.setId(UUID.randomUUID());
		review.setUser(user);
		review.setBook(book);
		review.setRating(rating);
		review.setComment("Comment");
		return review;
	}

	private Book publishedBook(User author) {
		Book book = new Book();
		book.setId(UUID.randomUUID());
		book.setAuthor(author);
		book.setTitle("Published book");
		book.setGenre("Fantasy");
		book.setStatus(BookStatus.PUBLISHED);
		book.setVisibility(BookVisibility.PUBLIC);
		book.setPublishedAt(LocalDateTime.now());
		return book;
	}

	private User user(String email) {
		User user = new User();
		user.setId(UUID.randomUUID());
		user.setEmail(email);
		user.setUsername(email.substring(0, email.indexOf('@')));
		return user;
	}
}
