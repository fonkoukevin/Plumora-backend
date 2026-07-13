package com.plumora.api.ai.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.plumora.api.ai.domain.AiRecommendationRequestEntity;
import com.plumora.api.ai.domain.AiRecommendationResult;
import com.plumora.api.ai.infrastructure.AiRecommendationRequestRepository;
import com.plumora.api.ai.infrastructure.AiRecommendationResultRepository;
import com.plumora.api.ai.infrastructure.provider.AiRecommendationCandidate;
import com.plumora.api.ai.infrastructure.provider.AiRecommendationPrompt;
import com.plumora.api.ai.infrastructure.provider.AiRecommendationProvider;
import com.plumora.api.ai.presentation.AiBookRecommendationRequest;
import com.plumora.api.ai.presentation.AiBookRecommendationResponse;
import com.plumora.api.ai.presentation.AiRecommendationRequest;
import com.plumora.api.book.domain.Book;
import com.plumora.api.book.domain.BookStatus;
import com.plumora.api.book.domain.BookVisibility;
import com.plumora.api.book.infrastructure.BookRepository;
import com.plumora.api.shared.exception.AiProviderUnavailableException;
import com.plumora.api.shared.exception.UnauthorizedActionException;
import com.plumora.api.user.application.UserService;
import com.plumora.api.user.domain.Role;
import com.plumora.api.user.domain.RoleName;
import com.plumora.api.user.domain.User;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class AiRecommendationServiceTest {

	@Mock
	private AiRecommendationRequestRepository requestRepository;

	@Mock
	private AiRecommendationResultRepository resultRepository;

	@Mock
	private BookRepository bookRepository;

	@Mock
	private UserService userService;

	@Mock
	private AiRecommendationProvider recommendationProvider;

	@Mock
	private AiUsageLimiter usageLimiter;

	private final AiFeatureToggle aiFeatureToggle = new AiFeatureToggle();

	private AiRecommendationService recommendationService;

	@BeforeEach
	void setUp() {
		recommendationService = new AiRecommendationService(
			requestRepository,
			resultRepository,
			bookRepository,
			userService,
			recommendationProvider,
			usageLimiter,
			aiFeatureToggle
		);
	}

	@Test
	void createRecommendationsFailsWhenAiIsDisabled() {
		aiFeatureToggle.setEnabled(false);
		AiRecommendationRequest request = new AiRecommendationRequest("roman d'aventure", "joyeux", "court", "aventure");

		assertThatThrownBy(() -> recommendationService.createRecommendations("reader@example.com", request))
			.isInstanceOf(AiProviderUnavailableException.class);
		verifyNoInteractions(userService);
	}

	@Test
	void createsRequestAndStoresRankedPublishedBookResults() {
		User reader = user("reader@example.com");
		Book fantasyBook = publishedBook("Le Royaume des Brumes", "Fantasy", 12);
		Book mysteryBook = publishedBook("Minuit sur le Port", "Mystery", 7);
		AiRecommendationRequest request = createRequest();

		when(userService.getCurrentUser(reader.getEmail())).thenReturn(reader);
		when(requestRepository.save(any(AiRecommendationRequestEntity.class))).thenAnswer(invocation -> {
			AiRecommendationRequestEntity savedRequest = invocation.getArgument(0);
			savedRequest.setId(UUID.randomUUID());
			savedRequest.setCreatedAt(LocalDateTime.now());
			return savedRequest;
		});
		when(bookRepository.findPublishedPublicBooksForRecommendations(
			eq(BookStatus.PUBLISHED),
			eq(BookVisibility.PUBLIC),
			any(Pageable.class)
		)).thenReturn(List.of(fantasyBook, mysteryBook));
		when(recommendationProvider.recommendBooks(any(AiRecommendationPrompt.class), eq(List.of(fantasyBook, mysteryBook))))
			.thenReturn(List.of(
				new AiRecommendationCandidate(fantasyBook, 88, List.of("Genre correspondant.")),
				new AiRecommendationCandidate(mysteryBook, 42, List.of("Livre publie disponible."))
			));
		when(resultRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

		AiRecommendationService.RecommendationBundle bundle = recommendationService.createRecommendations(
			reader.getEmail(),
			request
		);

		assertThat(bundle.request().getUser()).isEqualTo(reader);
		assertThat(bundle.request().getQueryText()).isEqualTo("aventure magie brume");
		assertThat(bundle.results()).hasSize(2);
		assertThat(bundle.results().get(0).getBook()).isEqualTo(fantasyBook);
		assertThat(bundle.results().get(0).getMatchScore()).isEqualTo(88);
		assertThat(bundle.results().get(0).getRankPosition()).isEqualTo(1);
		verify(bookRepository).findPublishedPublicBooksForRecommendations(
			eq(BookStatus.PUBLISHED),
			eq(BookVisibility.PUBLIC),
			any(Pageable.class)
		);
	}

	@Test
	void requestOwnerCanLoadStoredRecommendationRequest() {
		User reader = user("reader@example.com");
		AiRecommendationRequestEntity request = recommendationRequest(reader);
		AiRecommendationResult result = result(request, publishedBook("Livre", "Fantasy", 3));

		when(requestRepository.findByIdWithUser(request.getId())).thenReturn(Optional.of(request));
		when(resultRepository.findByRequestOrderByRankPositionAsc(request)).thenReturn(List.of(result));

		AiRecommendationService.RecommendationBundle bundle = recommendationService.getRequest(reader.getEmail(), request.getId());

		assertThat(bundle.request()).isEqualTo(request);
		assertThat(bundle.results()).containsExactly(result);
	}

	@Test
	void nonOwnerCannotLoadRecommendationRequest() {
		User reader = user("reader@example.com");
		User otherReader = user("other@example.com");
		AiRecommendationRequestEntity request = recommendationRequest(reader);

		when(requestRepository.findByIdWithUser(request.getId())).thenReturn(Optional.of(request));

		assertThatThrownBy(() -> recommendationService.getRequest(otherReader.getEmail(), request.getId()))
			.isInstanceOf(UnauthorizedActionException.class)
			.hasMessage("Only the request owner can access this AI recommendation request");
	}

	@Test
	void statelessRecommendationDoesNotPersistAnything() {
		User reader = user("reader@example.com");
		Book fantasyBook = publishedBook("Le Royaume des Brumes", "Fantasy", 12);
		AiBookRecommendationRequest request = new AiBookRecommendationRequest(
			"aventure magique",
			List.of("Fantasy"),
			List.of(),
			"fr",
			5
		);

		when(bookRepository.findPublishedPublicBooksForRecommendations(
			eq(BookStatus.PUBLISHED),
			eq(BookVisibility.PUBLIC),
			any(Pageable.class)
		)).thenReturn(List.of(fantasyBook));
		when(recommendationProvider.recommendBooks(any(AiRecommendationPrompt.class), eq(List.of(fantasyBook))))
			.thenReturn(List.of(new AiRecommendationCandidate(fantasyBook, 90, List.of("Genre correspondant."))));
		when(recommendationProvider.providerName()).thenReturn("mock");
		when(recommendationProvider.modelName()).thenReturn("local-heuristic");

		AiBookRecommendationResponse response = recommendationService.recommendBooksStateless(reader.getEmail(), request);

		assertThat(response.recommendations()).hasSize(1);
		assertThat(response.recommendations().get(0).bookId()).isEqualTo(fantasyBook.getId());
		assertThat(response.recommendations().get(0).score()).isEqualTo(90);
		verifyNoInteractions(requestRepository, resultRepository);
	}

	private AiRecommendationRequest createRequest() {
		return new AiRecommendationRequest(
			"aventure magie brume",
			"mysterieux",
			"court",
			"Fantasy"
		);
	}

	private AiRecommendationRequestEntity recommendationRequest(User user) {
		AiRecommendationRequestEntity request = new AiRecommendationRequestEntity();
		request.setId(UUID.randomUUID());
		request.setUser(user);
		request.setQueryText("magie");
		request.setMood("mysterieux");
		request.setPreferredDuration("court");
		request.setPreferredGenre("Fantasy");
		request.setCreatedAt(LocalDateTime.now());
		return request;
	}

	private AiRecommendationResult result(AiRecommendationRequestEntity request, Book book) {
		AiRecommendationResult result = new AiRecommendationResult();
		result.setId(UUID.randomUUID());
		result.setRequest(request);
		result.setBook(book);
		result.setMatchScore(80);
		result.setReasons(List.of("Correspond au genre prefere."));
		result.setRankPosition(1);
		return result;
	}

	private Book publishedBook(String title, String genre, int readingCount) {
		Book book = new Book();
		book.setId(UUID.randomUUID());
		book.setAuthor(user("author-" + UUID.randomUUID() + "@example.com"));
		book.setTitle(title);
		book.setSummary("Une aventure pleine de magie et de secrets.");
		book.setGenre(genre);
		book.setStatus(BookStatus.PUBLISHED);
		book.setVisibility(BookVisibility.PUBLIC);
		book.setPublishedAt(LocalDateTime.now());
		book.setReadingCount(readingCount);
		book.setAverageRating(BigDecimal.valueOf(4.5));
		return book;
	}

	private User user(String email) {
		User user = new User();
		user.setId(UUID.randomUUID());
		user.setFirstname("Test");
		user.setLastname("User");
		user.setEmail(email);
		user.setUsername(email.substring(0, email.indexOf('@')));
		Role role = new Role(RoleName.READER, RoleName.READER.name());
		role.setId(UUID.randomUUID());
		user.setRoles(Set.of(role));
		return user;
	}
}
