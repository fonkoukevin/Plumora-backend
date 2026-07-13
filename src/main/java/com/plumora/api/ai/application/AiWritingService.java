package com.plumora.api.ai.application;

import com.plumora.api.ai.domain.AiSuggestionStatus;
import com.plumora.api.ai.domain.AiWritingRequest;
import com.plumora.api.ai.domain.AiWritingSuggestion;
import com.plumora.api.ai.infrastructure.AiWritingRequestRepository;
import com.plumora.api.ai.infrastructure.AiWritingSuggestionRepository;
import com.plumora.api.ai.infrastructure.provider.AiProvider;
import com.plumora.api.ai.infrastructure.provider.AiProviderResponse;
import com.plumora.api.ai.infrastructure.provider.AiTextGenerationPrompt;
import com.plumora.api.ai.infrastructure.provider.AiTextGenerationResult;
import com.plumora.api.ai.infrastructure.provider.AiTitleSuggestionResult;
import com.plumora.api.ai.infrastructure.provider.AiWritingPrompt;
import com.plumora.api.ai.presentation.AiTextGenerationRequest;
import com.plumora.api.ai.presentation.AiTextGenerationResponse;
import com.plumora.api.ai.presentation.AiTitleSuggestionResponse;
import com.plumora.api.ai.presentation.CreateAiWritingSuggestionRequest;
import com.plumora.api.book.application.BookService;
import com.plumora.api.book.domain.Book;
import com.plumora.api.book.domain.Chapter;
import com.plumora.api.book.infrastructure.ChapterRepository;
import com.plumora.api.shared.exception.AiInputTooLargeException;
import com.plumora.api.shared.exception.AiUnauthorizedAccessException;
import com.plumora.api.shared.exception.ResourceNotFoundException;
import com.plumora.api.shared.exception.UnauthorizedActionException;
import com.plumora.api.user.application.UserService;
import com.plumora.api.user.domain.User;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AiWritingService {

	private final AiWritingRequestRepository requestRepository;
	private final AiWritingSuggestionRepository suggestionRepository;
	private final ChapterRepository chapterRepository;
	private final UserService userService;
	private final AiProvider aiProvider;
	private final BookService bookService;
	private final AiUsageLimiter usageLimiter;
	private final AiFeatureToggle aiFeatureToggle;
	private final int maxInputChars;

	public AiWritingService(
		AiWritingRequestRepository requestRepository,
		AiWritingSuggestionRepository suggestionRepository,
		ChapterRepository chapterRepository,
		UserService userService,
		AiProvider aiProvider,
		BookService bookService,
		AiUsageLimiter usageLimiter,
		AiFeatureToggle aiFeatureToggle,
		@Value("${app.ai.max-input-chars:12000}") int maxInputChars
	) {
		this.requestRepository = requestRepository;
		this.suggestionRepository = suggestionRepository;
		this.chapterRepository = chapterRepository;
		this.userService = userService;
		this.aiProvider = aiProvider;
		this.bookService = bookService;
		this.usageLimiter = usageLimiter;
		this.aiFeatureToggle = aiFeatureToggle;
		this.maxInputChars = maxInputChars;
	}

	@Transactional
	public AiWritingSuggestion createSuggestion(String currentUserEmail, CreateAiWritingSuggestionRequest request) {
		aiFeatureToggle.ensureEnabled();
		User currentUser = userService.getCurrentUser(currentUserEmail);
		Chapter chapter = findChapter(request.chapterId());
		ensureChapterAuthor(currentUserEmail, chapter);

		AiWritingRequest writingRequest = new AiWritingRequest();
		writingRequest.setUser(currentUser);
		writingRequest.setChapter(chapter);
		writingRequest.setSelectedText(request.selectedText());
		writingRequest.setContextText(request.contextText());
		writingRequest.setActionType(request.actionType());
		AiWritingRequest savedRequest = requestRepository.save(writingRequest);

		AiProviderResponse providerResponse = aiProvider.generateWritingSuggestion(new AiWritingPrompt(
			request.selectedText(),
			request.contextText(),
			request.actionType(),
			chapter.getTitle(),
			chapter.getBook().getTitle()
		));

		AiWritingSuggestion suggestion = new AiWritingSuggestion();
		suggestion.setRequest(savedRequest);
		suggestion.setSuggestionText(providerResponse.suggestionText());
		suggestion.setExplanation(providerResponse.explanation());
		suggestion.setStatus(AiSuggestionStatus.PENDING);
		return suggestionRepository.save(suggestion);
	}

	@Transactional(readOnly = true)
	public List<AiWritingRequest> getMyRequests(String currentUserEmail) {
		User currentUser = userService.getCurrentUser(currentUserEmail);
		return requestRepository.findByUserOrderByCreatedAtDesc(currentUser);
	}

	@Transactional(readOnly = true)
	public AiWritingRequest getRequest(String currentUserEmail, UUID requestId) {
		AiWritingRequest request = findRequest(requestId);
		ensureRequestOwner(currentUserEmail, request);
		return request;
	}

	@Transactional(readOnly = true)
	public List<AiWritingSuggestion> getRequestSuggestions(String currentUserEmail, AiWritingRequest request) {
		ensureRequestOwner(currentUserEmail, request);
		return suggestionRepository.findByRequestOrderByCreatedAtDesc(request);
	}

	@Transactional
	public AiWritingSuggestion acceptSuggestion(String currentUserEmail, UUID suggestionId) {
		return updateSuggestionStatus(currentUserEmail, suggestionId, AiSuggestionStatus.ACCEPTED);
	}

	@Transactional
	public AiWritingSuggestion modifySuggestion(String currentUserEmail, UUID suggestionId) {
		return updateSuggestionStatus(currentUserEmail, suggestionId, AiSuggestionStatus.MODIFIED);
	}

	@Transactional
	public AiWritingSuggestion ignoreSuggestion(String currentUserEmail, UUID suggestionId) {
		return updateSuggestionStatus(currentUserEmail, suggestionId, AiSuggestionStatus.IGNORED);
	}

	private AiWritingSuggestion updateSuggestionStatus(String currentUserEmail, UUID suggestionId, AiSuggestionStatus status) {
		AiWritingSuggestion suggestion = findSuggestion(suggestionId);
		ensureRequestOwner(currentUserEmail, suggestion.getRequest());
		suggestion.setStatus(status);
		return suggestionRepository.save(suggestion);
	}

	@Transactional(readOnly = true)
	public AiTextGenerationResponse rewrite(String currentUserEmail, AiTextGenerationRequest request) {
		return generate(currentUserEmail, request, aiProvider::rewriteText);
	}

	@Transactional(readOnly = true)
	public AiTextGenerationResponse summarize(String currentUserEmail, AiTextGenerationRequest request) {
		return generate(currentUserEmail, request, aiProvider::summarizeText);
	}

	@Transactional(readOnly = true)
	public AiTextGenerationResponse continueWriting(String currentUserEmail, AiTextGenerationRequest request) {
		return generate(currentUserEmail, request, aiProvider::continueText);
	}

	@Transactional(readOnly = true)
	public AiTitleSuggestionResponse suggestTitles(String currentUserEmail, AiTextGenerationRequest request) {
		aiFeatureToggle.ensureEnabled();
		usageLimiter.checkAndRecord(currentUserEmail);
		ensureWithinInputLimit(request.text());
		String contextTitle = resolveContextTitle(currentUserEmail, request.manuscriptId(), request.chapterId());

		AiTitleSuggestionResult result = aiProvider.suggestTitles(new AiTextGenerationPrompt(
			request.text(),
			request.language(),
			request.tone(),
			request.instruction(),
			contextTitle
		));

		return new AiTitleSuggestionResponse(
			result.titles(),
			result.explanation(),
			result.warnings(),
			aiProvider.providerName(),
			aiProvider.modelName(),
			LocalDateTime.now()
		);
	}

	private AiTextGenerationResponse generate(
		String currentUserEmail,
		AiTextGenerationRequest request,
		Function<AiTextGenerationPrompt, AiTextGenerationResult> operation
	) {
		aiFeatureToggle.ensureEnabled();
		usageLimiter.checkAndRecord(currentUserEmail);
		ensureWithinInputLimit(request.text());
		String contextTitle = resolveContextTitle(currentUserEmail, request.manuscriptId(), request.chapterId());

		AiTextGenerationResult result = operation.apply(new AiTextGenerationPrompt(
			request.text(),
			request.language(),
			request.tone(),
			request.instruction(),
			contextTitle
		));

		return new AiTextGenerationResponse(
			result.suggestion(),
			result.explanation(),
			result.warnings(),
			aiProvider.providerName(),
			aiProvider.modelName(),
			LocalDateTime.now()
		);
	}

	private void ensureWithinInputLimit(String text) {
		if (text != null && text.length() > maxInputChars) {
			throw new AiInputTooLargeException(
				"Text exceeds the maximum allowed length of " + maxInputChars + " characters"
			);
		}
	}

	private String resolveContextTitle(String currentUserEmail, UUID manuscriptId, UUID chapterId) {
		if (chapterId != null) {
			Chapter chapter = findChapter(chapterId);
			if (!chapter.getBook().getAuthor().getEmail().equals(currentUserEmail)) {
				throw new AiUnauthorizedAccessException("Only the chapter author can use Plumo IA on this chapter");
			}
			return chapter.getBook().getTitle() + " - " + chapter.getTitle();
		}
		if (manuscriptId != null) {
			Book book = bookService.getBook(manuscriptId);
			if (!book.getAuthor().getEmail().equals(currentUserEmail)) {
				throw new AiUnauthorizedAccessException("Only the book author can use Plumo IA on this manuscript");
			}
			return book.getTitle();
		}
		return null;
	}

	private Chapter findChapter(UUID chapterId) {
		return chapterRepository.findByIdWithBookAndAuthor(chapterId)
			.orElseThrow(() -> new ResourceNotFoundException("Chapter was not found"));
	}

	private AiWritingRequest findRequest(UUID requestId) {
		return requestRepository.findByIdWithDetails(requestId)
			.orElseThrow(() -> new ResourceNotFoundException("AI writing request was not found"));
	}

	private AiWritingSuggestion findSuggestion(UUID suggestionId) {
		return suggestionRepository.findByIdWithRequestDetails(suggestionId)
			.orElseThrow(() -> new ResourceNotFoundException("AI writing suggestion was not found"));
	}

	private void ensureChapterAuthor(String currentUserEmail, Chapter chapter) {
		if (!chapter.getBook().getAuthor().getEmail().equals(currentUserEmail)) {
			throw new UnauthorizedActionException("Only the chapter author can request AI writing suggestions");
		}
	}

	private void ensureRequestOwner(String currentUserEmail, AiWritingRequest request) {
		if (!request.getUser().getEmail().equals(currentUserEmail)) {
			throw new UnauthorizedActionException("Only the request owner can access this AI writing request");
		}
	}
}
