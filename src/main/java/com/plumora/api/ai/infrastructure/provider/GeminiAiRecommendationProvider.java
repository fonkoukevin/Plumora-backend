package com.plumora.api.ai.infrastructure.provider;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.plumora.api.ai.infrastructure.provider.gemini.GeminiClient;
import com.plumora.api.ai.infrastructure.provider.gemini.GeminiSystemPrompt;
import com.plumora.api.book.domain.Book;
import com.plumora.api.shared.exception.AiInvalidResponseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@ConditionalOnProperty(name = "plumora.ai.recommendation-provider", havingValue = "gemini")
public class GeminiAiRecommendationProvider implements AiRecommendationProvider {

	private static final Logger log = LoggerFactory.getLogger(GeminiAiRecommendationProvider.class);
	private static final int SUMMARY_MAX_LENGTH = 200;

	private final GeminiClient geminiClient;
	private final ObjectMapper objectMapper;

	public GeminiAiRecommendationProvider(GeminiClient geminiClient, ObjectMapper objectMapper) {
		this.geminiClient = geminiClient;
		this.objectMapper = objectMapper;
	}

	@Override
	public List<AiRecommendationCandidate> recommendBooks(AiRecommendationPrompt prompt, List<Book> candidates) {
		if (candidates.isEmpty()) {
			return List.of();
		}

		String userPrompt = buildPrompt(prompt, candidates);
		String json = geminiClient.generateJson(GeminiSystemPrompt.TEXT, userPrompt);
		GeminiRecommendationPayload payload = parse(json);

		Map<UUID, Book> candidatesById = candidates.stream()
			.collect(Collectors.toMap(Book::getId, Function.identity()));

		List<AiRecommendationCandidate> results = new ArrayList<>();
		if (payload.recommendations() != null) {
			for (GeminiRecommendationItem item : payload.recommendations()) {
				UUID bookId = parseUuid(item.bookId());
				Book book = bookId == null ? null : candidatesById.get(bookId);
				if (book == null) {
					log.warn("Gemini recommended a book id outside the candidate list, ignoring it");
					continue;
				}
				int score = Math.max(0, Math.min(item.score(), 100));
				String reason = StringUtils.hasText(item.reason()) ? item.reason() : "Recommande par Plumo IA.";
				results.add(new AiRecommendationCandidate(book, score, List.of(reason)));
			}
		}
		results.sort(Comparator.comparingInt(AiRecommendationCandidate::matchScore).reversed());
		return results;
	}

	@Override
	public String providerName() {
		return "gemini";
	}

	@Override
	public String modelName() {
		return geminiClient.model();
	}

	private String buildPrompt(AiRecommendationPrompt prompt, List<Book> candidates) {
		StringBuilder builder = new StringBuilder();
		builder.append("Voici une liste de livres disponibles dans le catalogue Plumora. ");
		builder.append("Tu dois recommander uniquement parmi ces livres, en utilisant leur id exact. ");
		builder.append("N'invente jamais de livre absent de cette liste.\n\n");
		builder.append("Preferences du lecteur :\n");
		if (StringUtils.hasText(prompt.queryText())) {
			builder.append("- Recherche : ").append(prompt.queryText()).append('\n');
		}
		if (StringUtils.hasText(prompt.mood())) {
			builder.append("- Humeur : ").append(prompt.mood()).append('\n');
		}
		if (StringUtils.hasText(prompt.preferredDuration())) {
			builder.append("- Duree preferee : ").append(prompt.preferredDuration()).append('\n');
		}
		if (StringUtils.hasText(prompt.preferredGenre())) {
			builder.append("- Genre prefere : ").append(prompt.preferredGenre()).append('\n');
		}
		builder.append("\nLivres disponibles :\n");
		for (Book book : candidates) {
			builder.append("- id: ").append(book.getId())
				.append(", titre: ").append(book.getTitle())
				.append(", genre: ").append(book.getGenre())
				.append(", resume: ").append(truncate(book.getSummary()))
				.append('\n');
		}
		builder.append("\nReponds uniquement avec un JSON valide de la forme : ")
			.append("{\"recommendations\": [{\"book_id\": \"...\", \"reason\": \"...\", \"score\": 0}]} ")
			.append("(score entre 0 et 100, uniquement des book_id presents dans la liste ci-dessus).");
		return builder.toString();
	}

	private String truncate(String text) {
		if (text == null) {
			return "";
		}
		String trimmed = text.trim();
		return trimmed.length() <= SUMMARY_MAX_LENGTH ? trimmed : trimmed.substring(0, SUMMARY_MAX_LENGTH) + "...";
	}

	private UUID parseUuid(String value) {
		try {
			return value == null ? null : UUID.fromString(value);
		} catch (IllegalArgumentException exception) {
			return null;
		}
	}

	private GeminiRecommendationPayload parse(String json) {
		try {
			return objectMapper.readValue(json, GeminiRecommendationPayload.class);
		} catch (JsonProcessingException exception) {
			throw new AiInvalidResponseException("Gemini returned a response that could not be parsed as JSON");
		}
	}

	private record GeminiRecommendationPayload(List<GeminiRecommendationItem> recommendations) {
	}

	private record GeminiRecommendationItem(
		@JsonProperty("book_id") String bookId,
		String reason,
		int score
	) {
	}
}
