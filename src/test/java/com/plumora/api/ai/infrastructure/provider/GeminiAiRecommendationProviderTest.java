package com.plumora.api.ai.infrastructure.provider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.plumora.api.ai.infrastructure.provider.gemini.GeminiClient;
import com.plumora.api.book.domain.Book;
import com.plumora.api.book.domain.BookStatus;
import com.plumora.api.book.domain.BookVisibility;
import com.plumora.api.user.domain.User;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GeminiAiRecommendationProviderTest {

	@Mock
	private GeminiClient geminiClient;

	private GeminiAiRecommendationProvider provider;

	@BeforeEach
	void setUp() {
		provider = new GeminiAiRecommendationProvider(geminiClient, new ObjectMapper());
	}

	@Test
	void ignoresBookIdsNotPresentInCandidateList() {
		Book realBook = book("Le Royaume des Brumes", "Fantasy");
		String invalidId = UUID.randomUUID().toString();
		String json = """
			{"recommendations": [
			  {"book_id": "%s", "reason": "Correspond au genre.", "score": 90},
			  {"book_id": "%s", "reason": "Livre invente qui ne doit pas apparaitre.", "score": 99}
			]}
			""".formatted(realBook.getId(), invalidId);

		when(geminiClient.generateJson(anyString(), anyString())).thenReturn(json);

		List<AiRecommendationCandidate> results = provider.recommendBooks(
			new AiRecommendationPrompt("magie", null, null, "Fantasy"),
			List.of(realBook)
		);

		assertThat(results).hasSize(1);
		assertThat(results.get(0).book()).isEqualTo(realBook);
		assertThat(results.get(0).matchScore()).isEqualTo(90);
	}

	@Test
	void returnsEmptyListWhenNoCandidates() {
		List<AiRecommendationCandidate> results = provider.recommendBooks(
			new AiRecommendationPrompt("magie", null, null, "Fantasy"),
			List.of()
		);

		assertThat(results).isEmpty();
	}

	private Book book(String title, String genre) {
		User author = new User();
		author.setId(UUID.randomUUID());
		author.setFirstname("Author");
		author.setLastname("Test");
		author.setUsername("author" + UUID.randomUUID());

		Book book = new Book();
		book.setId(UUID.randomUUID());
		book.setAuthor(author);
		book.setTitle(title);
		book.setSummary("Resume de test.");
		book.setGenre(genre);
		book.setStatus(BookStatus.PUBLISHED);
		book.setVisibility(BookVisibility.PUBLIC);
		return book;
	}
}
