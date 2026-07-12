package com.plumora.api.ai.application;

import com.plumora.api.ai.domain.AiRecommendationRequestEntity;
import com.plumora.api.ai.domain.AiRecommendationResult;
import com.plumora.api.ai.infrastructure.AiRecommendationRequestRepository;
import com.plumora.api.ai.infrastructure.AiRecommendationResultRepository;
import com.plumora.api.ai.infrastructure.provider.AiRecommendationCandidate;
import com.plumora.api.ai.infrastructure.provider.AiRecommendationPrompt;
import com.plumora.api.ai.infrastructure.provider.AiRecommendationProvider;
import com.plumora.api.ai.presentation.AiBookRecommendationItem;
import com.plumora.api.ai.presentation.AiBookRecommendationRequest;
import com.plumora.api.ai.presentation.AiBookRecommendationResponse;
import com.plumora.api.ai.presentation.AiRecommendationRequest;
import com.plumora.api.book.domain.Book;
import com.plumora.api.book.domain.BookStatus;
import com.plumora.api.book.domain.BookVisibility;
import com.plumora.api.book.infrastructure.BookRepository;
import com.plumora.api.shared.exception.ResourceNotFoundException;
import com.plumora.api.shared.exception.UnauthorizedActionException;
import com.plumora.api.user.application.UserService;
import com.plumora.api.user.domain.User;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AiRecommendationService {

	private static final int CANDIDATE_LIMIT = 50;
	private static final int DEFAULT_STATELESS_LIMIT = 10;
	private static final int MAX_STATELESS_LIMIT = 20;

	private final AiRecommendationRequestRepository requestRepository;
	private final AiRecommendationResultRepository resultRepository;
	private final BookRepository bookRepository;
	private final UserService userService;
	private final AiRecommendationProvider recommendationProvider;
	private final AiUsageLimiter usageLimiter;

	public AiRecommendationService(
		AiRecommendationRequestRepository requestRepository,
		AiRecommendationResultRepository resultRepository,
		BookRepository bookRepository,
		UserService userService,
		AiRecommendationProvider recommendationProvider,
		AiUsageLimiter usageLimiter
	) {
		this.requestRepository = requestRepository;
		this.resultRepository = resultRepository;
		this.bookRepository = bookRepository;
		this.userService = userService;
		this.recommendationProvider = recommendationProvider;
		this.usageLimiter = usageLimiter;
	}

	@Transactional
	public RecommendationBundle createRecommendations(String currentUserEmail, AiRecommendationRequest request) {
		User currentUser = userService.getCurrentUser(currentUserEmail);

		AiRecommendationRequestEntity recommendationRequest = new AiRecommendationRequestEntity();
		recommendationRequest.setUser(currentUser);
		recommendationRequest.setQueryText(request.queryText());
		recommendationRequest.setMood(request.mood());
		recommendationRequest.setPreferredDuration(request.preferredDuration());
		recommendationRequest.setPreferredGenre(request.preferredGenre());
		AiRecommendationRequestEntity savedRequest = requestRepository.save(recommendationRequest);

		List<Book> candidates = bookRepository.findPublishedPublicBooksForRecommendations(
			BookStatus.PUBLISHED,
			BookVisibility.PUBLIC,
			PageRequest.of(0, CANDIDATE_LIMIT, Sort.by(Sort.Direction.DESC, "readingCount")
				.and(Sort.by(Sort.Direction.DESC, "publishedAt")))
		);

		List<AiRecommendationCandidate> recommendations = recommendationProvider.recommendBooks(
			new AiRecommendationPrompt(
				request.queryText(),
				request.mood(),
				request.preferredDuration(),
				request.preferredGenre()
			),
			candidates
		);

		List<AiRecommendationResult> results = new ArrayList<>();
		int rank = 1;
		for (AiRecommendationCandidate recommendation : recommendations) {
			AiRecommendationResult result = new AiRecommendationResult();
			result.setRequest(savedRequest);
			result.setBook(recommendation.book());
			result.setMatchScore(recommendation.matchScore());
			result.setReasons(recommendation.reasons());
			result.setRankPosition(rank++);
			results.add(result);
		}

		List<AiRecommendationResult> savedResults = resultRepository.saveAll(results);
		return new RecommendationBundle(savedRequest, savedResults);
	}

	@Transactional(readOnly = true)
	public List<RecommendationBundle> getMyRequests(String currentUserEmail) {
		User currentUser = userService.getCurrentUser(currentUserEmail);
		return requestRepository.findByUserOrderByCreatedAtDesc(currentUser)
			.stream()
			.map(request -> new RecommendationBundle(
				request,
				resultRepository.findByRequestOrderByRankPositionAsc(request)
			))
			.toList();
	}

	@Transactional(readOnly = true)
	public RecommendationBundle getRequest(String currentUserEmail, UUID requestId) {
		AiRecommendationRequestEntity request = requestRepository.findByIdWithUser(requestId)
			.orElseThrow(() -> new ResourceNotFoundException("AI recommendation request was not found"));
		ensureRequestOwner(currentUserEmail, request);
		return new RecommendationBundle(
			request,
			resultRepository.findByRequestOrderByRankPositionAsc(request)
		);
	}

	private void ensureRequestOwner(String currentUserEmail, AiRecommendationRequestEntity request) {
		if (!request.getUser().getEmail().equals(currentUserEmail)) {
			throw new UnauthorizedActionException("Only the request owner can access this AI recommendation request");
		}
	}

	@Transactional(readOnly = true)
	public AiBookRecommendationResponse recommendBooksStateless(String currentUserEmail, AiBookRecommendationRequest request) {
		usageLimiter.checkAndRecord(currentUserEmail);

		Set<UUID> excludedBookIds = request.readingHistoryIds() == null
			? Set.of()
			: Set.copyOf(request.readingHistoryIds());
		String preferredGenre = request.favoriteGenres() == null || request.favoriteGenres().isEmpty()
			? null
			: request.favoriteGenres().get(0);
		int limit = resolveStatelessLimit(request.limit());

		List<Book> candidates = bookRepository.findPublishedPublicBooksForRecommendations(
			BookStatus.PUBLISHED,
			BookVisibility.PUBLIC,
			PageRequest.of(0, CANDIDATE_LIMIT, Sort.by(Sort.Direction.DESC, "readingCount")
				.and(Sort.by(Sort.Direction.DESC, "publishedAt")))
		).stream().filter(book -> !excludedBookIds.contains(book.getId())).toList();

		List<AiRecommendationCandidate> recommendations = recommendationProvider.recommendBooks(
			new AiRecommendationPrompt(request.userPreferences(), null, null, preferredGenre),
			candidates
		);

		List<AiBookRecommendationItem> items = recommendations.stream()
			.limit(limit)
			.map(candidate -> new AiBookRecommendationItem(
				candidate.book().getId(),
				candidate.book().getTitle(),
				String.join(" ", candidate.reasons()),
				candidate.matchScore()
			))
			.toList();

		return new AiBookRecommendationResponse(
			items,
			recommendationProvider.providerName(),
			recommendationProvider.modelName(),
			LocalDateTime.now()
		);
	}

	private int resolveStatelessLimit(Integer requestedLimit) {
		if (requestedLimit == null) {
			return DEFAULT_STATELESS_LIMIT;
		}
		return Math.max(1, Math.min(requestedLimit, MAX_STATELESS_LIMIT));
	}

	public record RecommendationBundle(
		AiRecommendationRequestEntity request,
		List<AiRecommendationResult> results
	) {
	}
}
