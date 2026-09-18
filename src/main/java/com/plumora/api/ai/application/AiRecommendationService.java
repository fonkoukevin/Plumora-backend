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
import com.plumora.api.book.application.ExternalBookService;
import com.plumora.api.book.application.ImportedExternalBookResult;
import com.plumora.api.book.domain.Book;
import com.plumora.api.book.domain.BookStatus;
import com.plumora.api.book.domain.BookVisibility;
import com.plumora.api.book.infrastructure.BookRepository;
import com.plumora.api.book.infrastructure.gutendex.GutendexBookResponse;
import com.plumora.api.book.infrastructure.gutendex.GutendexClient;
import com.plumora.api.book.infrastructure.gutendex.GutendexPageResponse;
import com.plumora.api.book.infrastructure.gutendex.GutendexSearchRequest;
import com.plumora.api.shared.exception.ResourceNotFoundException;
import com.plumora.api.shared.exception.UnauthorizedActionException;
import com.plumora.api.user.application.UserService;
import com.plumora.api.user.domain.User;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class AiRecommendationService {

	private static final Logger log = LoggerFactory.getLogger(AiRecommendationService.class);

	private static final int CANDIDATE_LIMIT = 50;
	private static final int DEFAULT_STATELESS_LIMIT = 10;
	private static final int MAX_STATELESS_LIMIT = 20;

	// How many NEW Gutendex books a single recommendation request is allowed to search for and
	// import on the fly, on top of whatever is already in the catalog. Kept small and
	// synchronous, same reasoning as AdminService#bulkImportGutendexBooks: each import
	// downloads a book's full text, and this runs inline in a request a user is actively
	// waiting on (the "Me recommander" button), not a background job.
	private static final int MAX_GUTENDEX_CANDIDATES_PER_REQUEST = 5;

	private final AiRecommendationRequestRepository requestRepository;
	private final AiRecommendationResultRepository resultRepository;
	private final BookRepository bookRepository;
	private final UserService userService;
	private final AiRecommendationProvider recommendationProvider;
	private final AiUsageLimiter usageLimiter;
	private final AiFeatureToggle aiFeatureToggle;
	private final GutendexClient gutendexClient;
	private final ExternalBookService externalBookService;

	public AiRecommendationService(
		AiRecommendationRequestRepository requestRepository,
		AiRecommendationResultRepository resultRepository,
		BookRepository bookRepository,
		UserService userService,
		AiRecommendationProvider recommendationProvider,
		AiUsageLimiter usageLimiter,
		AiFeatureToggle aiFeatureToggle,
		GutendexClient gutendexClient,
		ExternalBookService externalBookService
	) {
		this.requestRepository = requestRepository;
		this.resultRepository = resultRepository;
		this.bookRepository = bookRepository;
		this.userService = userService;
		this.recommendationProvider = recommendationProvider;
		this.usageLimiter = usageLimiter;
		this.aiFeatureToggle = aiFeatureToggle;
		this.gutendexClient = gutendexClient;
		this.externalBookService = externalBookService;
	}

	@Transactional
	public RecommendationBundle createRecommendations(String currentUserEmail, AiRecommendationRequest request) {
		aiFeatureToggle.ensureEnabled();
		User currentUser = userService.getCurrentUser(currentUserEmail);

		AiRecommendationRequestEntity recommendationRequest = new AiRecommendationRequestEntity();
		recommendationRequest.setUser(currentUser);
		recommendationRequest.setQueryText(request.queryText());
		recommendationRequest.setMood(request.mood());
		recommendationRequest.setPreferredDuration(request.preferredDuration());
		recommendationRequest.setPreferredGenre(request.preferredGenre());
		AiRecommendationRequestEntity savedRequest = requestRepository.save(recommendationRequest);

		List<Book> candidates = new ArrayList<>(bookRepository.findPublishedPublicBooksForRecommendations(
			BookStatus.PUBLISHED,
			BookVisibility.PUBLIC,
			PageRequest.of(0, CANDIDATE_LIMIT, Sort.by(Sort.Direction.DESC, "readingCount")
				.and(Sort.by(Sort.Direction.DESC, "publishedAt")))
		));
		Set<UUID> candidateIds = candidates.stream().map(Book::getId).collect(Collectors.toCollection(HashSet::new));
		for (Book gutendexMatch : findAndImportMatchingGutendexBooks(currentUserEmail, request.queryText())) {
			// A Gutendex search can resurface a book already imported from a previous request
			// (importGutendexBook returns the existing row rather than re-downloading it) -
			// that book is already in `candidates` from the query above, so skip it here
			// instead of scoring and potentially recommending the same book twice.
			if (candidateIds.add(gutendexMatch.getId())) {
				candidates.add(gutendexMatch);
			}
		}

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

	/**
	 * Searches Gutendex for up to {@link #MAX_GUTENDEX_CANDIDATES_PER_REQUEST} books matching
	 * the visitor's own query text and imports them, so Plumo can recommend from the whole
	 * public-domain catalog on Gutendex, not only whatever has already been imported before -
	 * on top of the demo/native catalog, which is what {@link #createRecommendations} already
	 * searches. Books already imported (by anyone, for any past request) are reused as-is
	 * rather than re-downloaded (see {@link ExternalBookService#importGutendexBook}).
	 * <p>
	 * Deliberately best-effort: a blank query, an empty Gutendex result set, or Gutendex/a
	 * single book being unavailable all just mean fewer (possibly zero) extra candidates - none
	 * of that should ever fail the recommendation itself, which still has the local catalog to
	 * fall back on.
	 */
	private List<Book> findAndImportMatchingGutendexBooks(String currentUserEmail, String queryText) {
		if (!StringUtils.hasText(queryText)) {
			return List.of();
		}

		GutendexPageResponse response;
		try {
			response = gutendexClient.searchBooks(GutendexSearchRequest.publicDomain(queryText, null, null, 1));
		} catch (RuntimeException exception) {
			log.warn("Gutendex search for AI recommendations failed, continuing with the local catalog only", exception);
			return List.of();
		}

		List<Book> imported = new ArrayList<>();
		for (GutendexBookResponse candidate : response.results()) {
			if (imported.size() >= MAX_GUTENDEX_CANDIDATES_PER_REQUEST) {
				break;
			}
			try {
				ImportedExternalBookResult result = externalBookService.importGutendexBook(currentUserEmail, candidate.id());
				imported.add(result.book());
			} catch (RuntimeException exception) {
				log.warn("Could not import Gutendex book {} for an AI recommendation", candidate.id(), exception);
			}
		}
		return imported;
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
		aiFeatureToggle.ensureEnabled();
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
