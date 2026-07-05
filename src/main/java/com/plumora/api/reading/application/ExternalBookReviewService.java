package com.plumora.api.reading.application;

import com.plumora.api.book.domain.ExternalBookSource;
import com.plumora.api.reading.domain.ExternalBookReview;
import com.plumora.api.reading.infrastructure.ExternalBookReviewRepository;
import com.plumora.api.reading.presentation.ReviewRequest;
import com.plumora.api.shared.exception.BusinessException;
import com.plumora.api.user.application.UserService;
import com.plumora.api.user.domain.User;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class ExternalBookReviewService {

	private final ExternalBookReviewRepository externalBookReviewRepository;
	private final UserService userService;

	public ExternalBookReviewService(
		ExternalBookReviewRepository externalBookReviewRepository,
		UserService userService
	) {
		this.externalBookReviewRepository = externalBookReviewRepository;
		this.userService = userService;
	}

	@Transactional(readOnly = true)
	public List<ExternalBookReview> getGutendexReviews(int gutendexId) {
		return externalBookReviewRepository.findByExternalSourceAndExternalIdOrderByCreatedAtDesc(
			ExternalBookSource.GUTENDEX,
			externalId(gutendexId)
		);
	}

	@Transactional
	public ExternalBookReview createGutendexReview(String currentUserEmail, int gutendexId, ReviewRequest request) {
		User user = userService.getCurrentUser(currentUserEmail);
		ExternalBookReview review = new ExternalBookReview();
		review.setUser(user);
		review.setExternalSource(ExternalBookSource.GUTENDEX);
		review.setExternalId(externalId(gutendexId));
		applyReviewFields(review, request);
		return externalBookReviewRepository.save(review);
	}

	private void applyReviewFields(ExternalBookReview review, ReviewRequest request) {
		if (request.rating() == null || request.rating() < 1 || request.rating() > 5) {
			throw new BusinessException("Rating must be between 1 and 5");
		}
		review.setRating(request.rating());
		review.setComment(request.comment());
	}

	private String externalId(int gutendexId) {
		if (gutendexId <= 0) {
			throw new BusinessException("Gutendex id must be positive");
		}
		String externalId = String.valueOf(gutendexId);
		if (!StringUtils.hasText(externalId)) {
			throw new BusinessException("Gutendex id is required");
		}
		return externalId;
	}
}
