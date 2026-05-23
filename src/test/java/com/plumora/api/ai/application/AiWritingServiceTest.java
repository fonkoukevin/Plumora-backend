package com.plumora.api.ai.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.plumora.api.ai.domain.AiSuggestionStatus;
import com.plumora.api.ai.domain.AiWritingActionType;
import com.plumora.api.ai.domain.AiWritingRequest;
import com.plumora.api.ai.domain.AiWritingSuggestion;
import com.plumora.api.ai.infrastructure.AiWritingRequestRepository;
import com.plumora.api.ai.infrastructure.AiWritingSuggestionRepository;
import com.plumora.api.ai.infrastructure.provider.AiProvider;
import com.plumora.api.ai.infrastructure.provider.AiProviderResponse;
import com.plumora.api.ai.infrastructure.provider.AiWritingPrompt;
import com.plumora.api.ai.presentation.CreateAiWritingSuggestionRequest;
import com.plumora.api.book.domain.Book;
import com.plumora.api.book.domain.BookStatus;
import com.plumora.api.book.domain.BookVisibility;
import com.plumora.api.book.domain.Chapter;
import com.plumora.api.book.infrastructure.ChapterRepository;
import com.plumora.api.shared.exception.UnauthorizedActionException;
import com.plumora.api.user.application.UserService;
import com.plumora.api.user.domain.Role;
import com.plumora.api.user.domain.RoleName;
import com.plumora.api.user.domain.User;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AiWritingServiceTest {

	@Mock
	private AiWritingRequestRepository requestRepository;

	@Mock
	private AiWritingSuggestionRepository suggestionRepository;

	@Mock
	private ChapterRepository chapterRepository;

	@Mock
	private UserService userService;

	@Mock
	private AiProvider aiProvider;

	private AiWritingService aiWritingService;

	@BeforeEach
	void setUp() {
		aiWritingService = new AiWritingService(
			requestRepository,
			suggestionRepository,
			chapterRepository,
			userService,
			aiProvider
		);
	}

	@Test
	void chapterAuthorCanCreateSuggestion() {
		User author = user("author@example.com");
		Chapter chapter = chapter(book(author));
		CreateAiWritingSuggestionRequest request = createRequest(chapter.getId());

		when(userService.getCurrentUser(author.getEmail())).thenReturn(author);
		when(chapterRepository.findByIdWithBookAndAuthor(chapter.getId())).thenReturn(Optional.of(chapter));
		when(requestRepository.save(any(AiWritingRequest.class))).thenAnswer(invocation -> {
			AiWritingRequest writingRequest = invocation.getArgument(0);
			writingRequest.setId(UUID.randomUUID());
			writingRequest.setCreatedAt(LocalDateTime.now());
			return writingRequest;
		});
		when(aiProvider.generateWritingSuggestion(any(AiWritingPrompt.class)))
			.thenReturn(new AiProviderResponse("Texte ameliore", "Suggestion fake locale."));
		when(suggestionRepository.save(any(AiWritingSuggestion.class))).thenAnswer(invocation -> {
			AiWritingSuggestion suggestion = invocation.getArgument(0);
			suggestion.setId(UUID.randomUUID());
			suggestion.setCreatedAt(LocalDateTime.now());
			return suggestion;
		});

		AiWritingSuggestion suggestion = aiWritingService.createSuggestion(author.getEmail(), request);

		assertThat(suggestion.getRequest().getUser()).isEqualTo(author);
		assertThat(suggestion.getRequest().getChapter()).isEqualTo(chapter);
		assertThat(suggestion.getRequest().getActionType()).isEqualTo(AiWritingActionType.IMPROVE_STYLE);
		assertThat(suggestion.getSuggestionText()).isEqualTo("Texte ameliore");
		assertThat(suggestion.getStatus()).isEqualTo(AiSuggestionStatus.PENDING);
		verify(aiProvider).generateWritingSuggestion(any(AiWritingPrompt.class));
	}

	@Test
	void anotherAuthorCannotCreateSuggestionForChapter() {
		User author = user("author@example.com");
		User otherAuthor = user("other@example.com");
		Chapter chapter = chapter(book(author));
		CreateAiWritingSuggestionRequest request = createRequest(chapter.getId());

		when(userService.getCurrentUser(otherAuthor.getEmail())).thenReturn(otherAuthor);
		when(chapterRepository.findByIdWithBookAndAuthor(chapter.getId())).thenReturn(Optional.of(chapter));

		assertThatThrownBy(() -> aiWritingService.createSuggestion(otherAuthor.getEmail(), request))
			.isInstanceOf(UnauthorizedActionException.class)
			.hasMessage("Only the chapter author can request AI writing suggestions");
	}

	@Test
	void acceptingSuggestionOnlyChangesSuggestionStatus() {
		User author = user("author@example.com");
		Chapter chapter = chapter(book(author));
		chapter.setContent("Original chapter content.");
		AiWritingSuggestion suggestion = suggestion(request(author, chapter));

		when(suggestionRepository.findByIdWithRequestDetails(suggestion.getId())).thenReturn(Optional.of(suggestion));
		when(suggestionRepository.save(suggestion)).thenReturn(suggestion);

		AiWritingSuggestion updated = aiWritingService.acceptSuggestion(author.getEmail(), suggestion.getId());

		assertThat(updated.getStatus()).isEqualTo(AiSuggestionStatus.ACCEPTED);
		assertThat(chapter.getContent()).isEqualTo("Original chapter content.");
	}

	@Test
	void nonOwnerCannotUpdateSuggestionStatus() {
		User author = user("author@example.com");
		User otherAuthor = user("other@example.com");
		Chapter chapter = chapter(book(author));
		AiWritingSuggestion suggestion = suggestion(request(author, chapter));

		when(suggestionRepository.findByIdWithRequestDetails(suggestion.getId())).thenReturn(Optional.of(suggestion));

		assertThatThrownBy(() -> aiWritingService.ignoreSuggestion(otherAuthor.getEmail(), suggestion.getId()))
			.isInstanceOf(UnauthorizedActionException.class)
			.hasMessage("Only the request owner can access this AI writing request");
	}

	private CreateAiWritingSuggestionRequest createRequest(UUID chapterId) {
		return new CreateAiWritingSuggestionRequest(
			chapterId,
			"Le vent etait tres froid.",
			"Scene d'ouverture dans une ville portuaire.",
			AiWritingActionType.IMPROVE_STYLE
		);
	}

	private AiWritingRequest request(User user, Chapter chapter) {
		AiWritingRequest request = new AiWritingRequest();
		request.setId(UUID.randomUUID());
		request.setUser(user);
		request.setChapter(chapter);
		request.setSelectedText("Le vent etait tres froid.");
		request.setContextText("Scene d'ouverture.");
		request.setActionType(AiWritingActionType.IMPROVE_STYLE);
		request.setCreatedAt(LocalDateTime.now());
		return request;
	}

	private AiWritingSuggestion suggestion(AiWritingRequest request) {
		AiWritingSuggestion suggestion = new AiWritingSuggestion();
		suggestion.setId(UUID.randomUUID());
		suggestion.setRequest(request);
		suggestion.setSuggestionText("Le vent mordait la peau.");
		suggestion.setExplanation("Style renforce.");
		suggestion.setStatus(AiSuggestionStatus.PENDING);
		suggestion.setCreatedAt(LocalDateTime.now());
		return suggestion;
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
		Role role = new Role(RoleName.AUTHOR, RoleName.AUTHOR.name());
		role.setId(UUID.randomUUID());
		user.setRoles(Set.of(role));
		return user;
	}
}
