package com.plumora.api.ai.presentation;

import com.plumora.api.ai.application.AiRecommendationService;
import jakarta.validation.Valid;
import java.security.Principal;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/ai/books")
public class AiBookRecommendationController {

	private final AiRecommendationService recommendationService;

	public AiBookRecommendationController(AiRecommendationService recommendationService) {
		this.recommendationService = recommendationService;
	}

	@PostMapping("/recommend")
	@ResponseStatus(HttpStatus.OK)
	@PreAuthorize("hasRole('READER')")
	public AiBookRecommendationResponse recommend(
		Principal principal,
		@Valid @RequestBody AiBookRecommendationRequest request
	) {
		return recommendationService.recommendBooksStateless(principal.getName(), request);
	}
}
