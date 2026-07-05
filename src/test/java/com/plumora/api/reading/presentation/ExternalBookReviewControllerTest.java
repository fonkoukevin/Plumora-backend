package com.plumora.api.reading.presentation;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.plumora.api.book.domain.ExternalBookSource;
import com.plumora.api.reading.application.ExternalBookReviewService;
import com.plumora.api.reading.application.ReviewService;
import com.plumora.api.reading.domain.ExternalBookReview;
import com.plumora.api.user.domain.User;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class ExternalBookReviewControllerTest {

	private final ObjectMapper objectMapper = new ObjectMapper();

	@Test
	void getExternalBookReviewsReturnsReviewsForGutendexBook() throws Exception {
		ReviewService reviewService = org.mockito.Mockito.mock(ReviewService.class);
		ExternalBookReviewService externalBookReviewService = org.mockito.Mockito.mock(ExternalBookReviewService.class);
		MockMvc mockMvc = MockMvcBuilders
			.standaloneSetup(new ReviewController(reviewService, externalBookReviewService))
			.build();
		when(externalBookReviewService.getGutendexReviews(2701)).thenReturn(List.of(review()));

		mockMvc.perform(get("/api/v1/external-books/2701/reviews")
				.contextPath("/api/v1"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$[0].externalId").value("2701"))
			.andExpect(jsonPath("$[0].source").value("GUTENDEX"))
			.andExpect(jsonPath("$[0].username").value("reader"))
			.andExpect(jsonPath("$[0].rating").value(5))
			.andExpect(jsonPath("$[0].comment").value("Excellent classique."));
	}

	@Test
	void postExternalBookReviewCreatesReviewForGutendexBook() throws Exception {
		ReviewService reviewService = org.mockito.Mockito.mock(ReviewService.class);
		ExternalBookReviewService externalBookReviewService = org.mockito.Mockito.mock(ExternalBookReviewService.class);
		MockMvc mockMvc = MockMvcBuilders
			.standaloneSetup(new ReviewController(reviewService, externalBookReviewService))
			.build();
		when(externalBookReviewService.createGutendexReview(
			org.mockito.Mockito.eq("reader@example.com"),
			org.mockito.Mockito.eq(2701),
			any(ReviewRequest.class)
		)).thenReturn(review());

		mockMvc.perform(post("/api/v1/external-books/2701/reviews")
				.contextPath("/api/v1")
				.principal(() -> "reader@example.com")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(new ReviewRequest(5, "Excellent classique."))))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.externalId").value("2701"))
			.andExpect(jsonPath("$.source").value("GUTENDEX"))
			.andExpect(jsonPath("$.rating").value(5));
	}

	private ExternalBookReview review() {
		User user = new User();
		user.setId(UUID.randomUUID());
		user.setUsername("reader");
		user.setEmail("reader@example.com");

		ExternalBookReview review = new ExternalBookReview();
		review.setId(UUID.randomUUID());
		review.setUser(user);
		review.setExternalSource(ExternalBookSource.GUTENDEX);
		review.setExternalId("2701");
		review.setRating(5);
		review.setComment("Excellent classique.");
		review.setCreatedAt(LocalDateTime.of(2026, 7, 4, 10, 0));
		return review;
	}
}
