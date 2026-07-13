package com.plumora.api.ai.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.plumora.api.ai.infrastructure.provider.AiBetaReadingPrompt;
import com.plumora.api.ai.infrastructure.provider.AiBetaReadingResult;
import com.plumora.api.ai.infrastructure.provider.AiProvider;
import com.plumora.api.ai.presentation.AiBetaReadingAnalysisRequest;
import com.plumora.api.ai.presentation.AiBetaReadingAnalysisResponse;
import com.plumora.api.book.application.BookService;
import com.plumora.api.book.domain.Book;
import com.plumora.api.book.domain.BookStatus;
import com.plumora.api.book.domain.BookVisibility;
import com.plumora.api.book.domain.Chapter;
import com.plumora.api.book.infrastructure.ChapterRepository;
import com.plumora.api.shared.exception.AiInputTooLargeException;
import com.plumora.api.shared.exception.AiProviderUnavailableException;
import com.plumora.api.shared.exception.AiUnauthorizedAccessException;
import com.plumora.api.user.domain.User;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AiBetaReadingAnalysisServiceTest {

	private static final int MAX_INPUT_CHARS = 12000;

	@Mock
	private ChapterRepository chapterRepository;

	@Mock
	private BookService bookService;

	@Mock
	private AiProvider aiProvider;

	@Mock
	private AiUsageLimiter usageLimiter;

	private final AiFeatureToggle aiFeatureToggle = new AiFeatureToggle();

	private AiBetaReadingAnalysisService analysisService;

	@BeforeEach
	void setUp() {
		analysisService = new AiBetaReadingAnalysisService(
			chapterRepository,
			bookService,
			aiProvider,
			usageLimiter,
			aiFeatureToggle,
			MAX_INPUT_CHARS
		);
	}

	@Test
	void analyzeFailsWhenAiIsDisabled() {
		aiFeatureToggle.setEnabled(false);
		AiBetaReadingAnalysisRequest request = new AiBetaReadingAnalysisRequest(
			"Un extrait de roman a analyser.",
			"fr",
			"drame",
			"detailed",
			null,
			null
		);

		assertThatThrownBy(() -> analysisService.analyze("reader@example.com", request))
			.isInstanceOf(AiProviderUnavailableException.class);
	}

	@Test
	void analyzeReturnsStructuredFeedback() {
		User author = user("author@example.com");
		AiBetaReadingAnalysisRequest request = new AiBetaReadingAnalysisRequest(
			"Un extrait de roman a analyser.",
			"fr",
			"Fantasy",
			"detaille",
			null,
			null
		);

		when(aiProvider.analyzeForBetaReading(any(AiBetaReadingPrompt.class))).thenReturn(new AiBetaReadingResult(
			"Retour global positif.",
			List.of("Voix narrative forte."),
			List.of("Rythme a resserrer."),
			8,
			7,
			9,
			6,
			List.of("Retravailler le deuxieme paragraphe."),
			List.of()
		));
		when(aiProvider.providerName()).thenReturn("mock");
		when(aiProvider.modelName()).thenReturn("local-heuristic");

		AiBetaReadingAnalysisResponse response = analysisService.analyze(author.getEmail(), request);

		assertThat(response.globalFeedback()).isEqualTo("Retour global positif.");
		assertThat(response.clarityScore()).isEqualTo(8);
		assertThat(response.provider()).isEqualTo("mock");
	}

	@Test
	void analyzeDeniesAccessWhenChapterNotOwnedByCurrentUser() {
		User author = user("author@example.com");
		User otherAuthor = user("other@example.com");
		Chapter chapter = chapter(book(author));
		AiBetaReadingAnalysisRequest request = new AiBetaReadingAnalysisRequest(
			"Un extrait de roman a analyser.",
			"fr",
			null,
			null,
			null,
			chapter.getId()
		);

		when(chapterRepository.findByIdWithBookAndAuthor(chapter.getId())).thenReturn(Optional.of(chapter));

		assertThatThrownBy(() -> analysisService.analyze(otherAuthor.getEmail(), request))
			.isInstanceOf(AiUnauthorizedAccessException.class)
			.hasMessage("Only the chapter author can use Plumo IA on this chapter");
	}

	@Test
	void analyzeThrowsWhenTextExceedsMaxInputChars() {
		User author = user("author@example.com");
		AiBetaReadingAnalysisRequest request = new AiBetaReadingAnalysisRequest(
			"a".repeat(MAX_INPUT_CHARS + 1),
			"fr",
			null,
			null,
			null,
			null
		);

		assertThatThrownBy(() -> analysisService.analyze(author.getEmail(), request))
			.isInstanceOf(AiInputTooLargeException.class);
	}

	private Chapter chapter(Book book) {
		Chapter chapter = new Chapter();
		chapter.setId(UUID.randomUUID());
		chapter.setBook(book);
		chapter.setTitle("Chapter 1");
		chapter.setContent("Chapter content");
		chapter.setChapterOrder(1);
		return chapter;
	}

	private Book book(User author) {
		Book book = new Book();
		book.setId(UUID.randomUUID());
		book.setAuthor(author);
		book.setTitle("Book title");
		book.setGenre("Fantasy");
		book.setStatus(BookStatus.DRAFT);
		book.setVisibility(BookVisibility.PRIVATE);
		return book;
	}

	private User user(String email) {
		User user = new User();
		user.setId(UUID.randomUUID());
		user.setFirstname("Test");
		user.setLastname("User");
		user.setEmail(email);
		user.setUsername(email.substring(0, email.indexOf('@')));
		return user;
	}
}
