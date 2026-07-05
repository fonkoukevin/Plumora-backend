package com.plumora.api.reading.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.plumora.api.book.domain.ExternalBookSource;
import com.plumora.api.reading.domain.ExternalBookReview;
import com.plumora.api.reading.infrastructure.ExternalBookReviewRepository;
import com.plumora.api.reading.presentation.ReviewRequest;
import com.plumora.api.shared.exception.BusinessException;
import com.plumora.api.user.application.UserService;
import com.plumora.api.user.domain.User;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ExternalBookReviewServiceTest {

	@Mock
	private ExternalBookReviewRepository externalBookReviewRepository;

	@Mock
	private UserService userService;

	private ExternalBookReviewService externalBookReviewService;

	@BeforeEach
	void setUp() {
		externalBookReviewService = new ExternalBookReviewService(externalBookReviewRepository, userService);
	}

	@Test
	void getGutendexReviewsLoadsReviewsBySourceAndExternalId() {
		ExternalBookReview review = review(user("reader@example.com"), "2701", 5);
		when(externalBookReviewRepository.findByExternalSourceAndExternalIdOrderByCreatedAtDesc(
			ExternalBookSource.GUTENDEX,
			"2701"
		)).thenReturn(List.of(review));

		List<ExternalBookReview> reviews = externalBookReviewService.getGutendexReviews(2701);

		assertThat(reviews).containsExactly(review);
	}

	@Test
	void createGutendexReviewPersistsReaderReview() {
		User reader = user("reader@example.com");
		when(userService.getCurrentUser(reader.getEmail())).thenReturn(reader);
		when(externalBookReviewRepository.save(any(ExternalBookReview.class)))
			.thenAnswer(invocation -> invocation.getArgument(0));

		ExternalBookReview savedReview = externalBookReviewService.createGutendexReview(
			reader.getEmail(),
			2701,
			new ReviewRequest(5, "Excellent classique.")
		);

		assertThat(savedReview.getUser()).isEqualTo(reader);
		assertThat(savedReview.getExternalSource()).isEqualTo(ExternalBookSource.GUTENDEX);
		assertThat(savedReview.getExternalId()).isEqualTo("2701");
		assertThat(savedReview.getRating()).isEqualTo(5);
		assertThat(savedReview.getComment()).isEqualTo("Excellent classique.");
		verify(externalBookReviewRepository).save(savedReview);
	}

	@Test
	void createGutendexReviewRejectsInvalidRating() {
		User reader = user("reader@example.com");
		when(userService.getCurrentUser(reader.getEmail())).thenReturn(reader);

		assertThatThrownBy(() -> externalBookReviewService.createGutendexReview(
			reader.getEmail(),
			2701,
			new ReviewRequest(6, "Too much")
		))
			.isInstanceOf(BusinessException.class)
			.hasMessage("Rating must be between 1 and 5");
	}

	@Test
	void getGutendexReviewsRejectsInvalidGutendexId() {
		assertThatThrownBy(() -> externalBookReviewService.getGutendexReviews(0))
			.isInstanceOf(BusinessException.class)
			.hasMessage("Gutendex id must be positive");
	}

	private ExternalBookReview review(User user, String externalId, int rating) {
		ExternalBookReview review = new ExternalBookReview();
		review.setId(UUID.randomUUID());
		review.setUser(user);
		review.setExternalSource(ExternalBookSource.GUTENDEX);
		review.setExternalId(externalId);
		review.setRating(rating);
		review.setComment("Comment");
		return review;
	}

	private User user(String email) {
		User user = new User();
		user.setId(UUID.randomUUID());
		user.setEmail(email);
		user.setUsername(email.substring(0, email.indexOf('@')));
		return user;
	}
}
