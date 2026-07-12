package com.plumora.api.ai.application;

import com.plumora.api.ai.infrastructure.provider.AiBetaReadingPrompt;
import com.plumora.api.ai.infrastructure.provider.AiBetaReadingResult;
import com.plumora.api.ai.infrastructure.provider.AiProvider;
import com.plumora.api.ai.presentation.AiBetaReadingAnalysisRequest;
import com.plumora.api.ai.presentation.AiBetaReadingAnalysisResponse;
import com.plumora.api.book.application.BookService;
import com.plumora.api.book.domain.Book;
import com.plumora.api.book.domain.Chapter;
import com.plumora.api.book.infrastructure.ChapterRepository;
import com.plumora.api.shared.exception.AiInputTooLargeException;
import com.plumora.api.shared.exception.AiUnauthorizedAccessException;
import com.plumora.api.shared.exception.ResourceNotFoundException;
import java.time.LocalDateTime;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AiBetaReadingAnalysisService {

	private final ChapterRepository chapterRepository;
	private final BookService bookService;
	private final AiProvider aiProvider;
	private final AiUsageLimiter usageLimiter;
	private final int maxInputChars;

	public AiBetaReadingAnalysisService(
		ChapterRepository chapterRepository,
		BookService bookService,
		AiProvider aiProvider,
		AiUsageLimiter usageLimiter,
		@Value("${app.ai.max-input-chars:12000}") int maxInputChars
	) {
		this.chapterRepository = chapterRepository;
		this.bookService = bookService;
		this.aiProvider = aiProvider;
		this.usageLimiter = usageLimiter;
		this.maxInputChars = maxInputChars;
	}

	@Transactional(readOnly = true)
	public AiBetaReadingAnalysisResponse analyze(String currentUserEmail, AiBetaReadingAnalysisRequest request) {
		usageLimiter.checkAndRecord(currentUserEmail);
		ensureWithinInputLimit(request.text());
		ensureContextOwnership(currentUserEmail, request.manuscriptId(), request.chapterId());

		AiBetaReadingResult result = aiProvider.analyzeForBetaReading(new AiBetaReadingPrompt(
			request.text(),
			request.language(),
			request.genre(),
			request.expectedFeedbackLevel()
		));

		return new AiBetaReadingAnalysisResponse(
			result.globalFeedback(),
			result.strengths(),
			result.weaknesses(),
			result.clarityScore(),
			result.rhythmScore(),
			result.coherenceScore(),
			result.characterScore(),
			result.suggestions(),
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

	private void ensureContextOwnership(String currentUserEmail, UUID manuscriptId, UUID chapterId) {
		if (chapterId != null) {
			Chapter chapter = chapterRepository.findByIdWithBookAndAuthor(chapterId)
				.orElseThrow(() -> new ResourceNotFoundException("Chapter was not found"));
			if (!chapter.getBook().getAuthor().getEmail().equals(currentUserEmail)) {
				throw new AiUnauthorizedAccessException("Only the chapter author can use Plumo IA on this chapter");
			}
			return;
		}
		if (manuscriptId != null) {
			Book book = bookService.getBook(manuscriptId);
			if (!book.getAuthor().getEmail().equals(currentUserEmail)) {
				throw new AiUnauthorizedAccessException("Only the book author can use Plumo IA on this manuscript");
			}
		}
	}
}
