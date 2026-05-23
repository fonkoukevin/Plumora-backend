package com.plumora.api.ai.infrastructure.provider;

import com.plumora.api.book.domain.Book;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@ConditionalOnProperty(name = "plumora.ai.recommendation-provider", havingValue = "fake", matchIfMissing = true)
public class FakeAiRecommendationProvider implements AiRecommendationProvider {

	private static final int MAX_RESULTS = 10;
	private static final Set<String> STOP_WORDS = Set.of(
		"avec", "dans", "pour", "une", "des", "les", "que", "qui", "sur", "sans",
		"the", "and", "for", "with", "from", "this", "that"
	);

	@Override
	public List<AiRecommendationCandidate> recommendBooks(AiRecommendationPrompt prompt, List<Book> candidates) {
		Set<String> keywords = keywords(prompt.queryText());
		String preferredGenre = normalize(prompt.preferredGenre());

		return candidates.stream()
			.map(book -> scoreBook(book, keywords, preferredGenre))
			.sorted(Comparator
				.comparingInt(AiRecommendationCandidate::matchScore).reversed()
				.thenComparing(candidate -> candidate.book().getReadingCount(), Comparator.reverseOrder())
				.thenComparing(candidate -> candidate.book().getAverageRating(), Comparator.reverseOrder())
				.thenComparing(candidate -> candidate.book().getPublishedAt(), Comparator.nullsLast(Comparator.reverseOrder())))
			.limit(MAX_RESULTS)
			.toList();
	}

	private AiRecommendationCandidate scoreBook(Book book, Set<String> keywords, String preferredGenre) {
		int score = 20;
		List<String> reasons = new ArrayList<>();

		if (StringUtils.hasText(preferredGenre) && normalize(book.getGenre()).equals(preferredGenre)) {
			score += 35;
			reasons.add("Correspond au genre prefere : " + book.getGenre() + ".");
		}

		int keywordMatches = keywordMatches(book, keywords);
		if (keywordMatches > 0) {
			score += Math.min(keywordMatches * 12, 36);
			reasons.add("Contient des themes proches de votre recherche.");
		}

		if (book.getReadingCount() > 0) {
			score += Math.min(book.getReadingCount(), 10);
			reasons.add("Deja lu par plusieurs lecteurs Plumora.");
		}

		if (book.getAverageRating() != null && book.getAverageRating().doubleValue() >= 4.0) {
			score += 10;
			reasons.add("Bien note par les lecteurs.");
		}

		if (reasons.isEmpty()) {
			reasons.add("Livre publie disponible dans le catalogue Plumora.");
		}

		return new AiRecommendationCandidate(book, Math.min(score, 100), reasons);
	}

	private int keywordMatches(Book book, Set<String> keywords) {
		if (keywords.isEmpty()) {
			return 0;
		}
		String searchable = normalize(book.getTitle() + " " + nullToBlank(book.getSummary()));
		int matches = 0;
		for (String keyword : keywords) {
			if (searchable.contains(keyword)) {
				matches++;
			}
		}
		return matches;
	}

	private Set<String> keywords(String text) {
		Set<String> keywords = new HashSet<>();
		for (String token : normalize(nullToBlank(text)).split("\\s+")) {
			if (token.length() >= 3 && !STOP_WORDS.contains(token)) {
				keywords.add(token);
			}
		}
		return keywords;
	}

	private String normalize(String text) {
		if (text == null) {
			return "";
		}
		String normalized = Normalizer.normalize(text, Normalizer.Form.NFD)
			.replaceAll("\\p{M}", "");
		return normalized.toLowerCase(Locale.ROOT).trim();
	}

	private String nullToBlank(String value) {
		return value == null ? "" : value;
	}
}
