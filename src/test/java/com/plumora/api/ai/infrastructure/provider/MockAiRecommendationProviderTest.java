package com.plumora.api.ai.infrastructure.provider;

import static org.assertj.core.api.Assertions.assertThat;

import com.plumora.api.book.domain.Book;
import com.plumora.api.book.domain.BookStatus;
import com.plumora.api.book.domain.BookVisibility;
import com.plumora.api.user.domain.User;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class MockAiRecommendationProviderTest {

	private final MockAiRecommendationProvider provider = new MockAiRecommendationProvider();

	@Test
	void genreAndKeywordMatchesAreRankedFirst() {
		Book matchingBook = book(
			"Le Royaume des Brumes",
			"Fantasy",
			"Une aventure de magie ancienne dans une foret mysterieuse.",
			5
		);
		Book fallbackBook = book(
			"Carnets du Rivage",
			"Romance",
			"Une histoire intime pres de la mer.",
			40
		);

		List<AiRecommendationCandidate> recommendations = provider.recommendBooks(
			new AiRecommendationPrompt("magie foret", "mysterieux", "court", "Fantasy"),
			List.of(fallbackBook, matchingBook)
		);

		assertThat(recommendations).hasSize(2);
		assertThat(recommendations.get(0).book()).isEqualTo(matchingBook);
		assertThat(recommendations.get(0).matchScore()).isGreaterThan(recommendations.get(1).matchScore());
		assertThat(recommendations.get(0).reasons()).anyMatch(reason -> reason.contains("genre prefere"));
		assertThat(recommendations.get(0).reasons()).anyMatch(reason -> reason.contains("themes proches"));
	}

	private Book book(String title, String genre, String summary, int readingCount) {
		User author = new User();
		author.setId(UUID.randomUUID());
		author.setFirstname("Author");
		author.setLastname("Test");
		author.setUsername("author" + UUID.randomUUID());

		Book book = new Book();
		book.setId(UUID.randomUUID());
		book.setAuthor(author);
		book.setTitle(title);
		book.setSummary(summary);
		book.setGenre(genre);
		book.setStatus(BookStatus.PUBLISHED);
		book.setVisibility(BookVisibility.PUBLIC);
		book.setPublishedAt(LocalDateTime.now());
		book.setReadingCount(readingCount);
		book.setAverageRating(BigDecimal.valueOf(4.2));
		return book;
	}
}
