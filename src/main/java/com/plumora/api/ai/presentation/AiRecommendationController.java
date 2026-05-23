package com.plumora.api.ai.presentation;

import com.plumora.api.ai.application.AiRecommendationService;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/ai/recommendations")
public class AiRecommendationController {

	private final AiRecommendationService recommendationService;

	public AiRecommendationController(AiRecommendationService recommendationService) {
		this.recommendationService = recommendationService;
	}

	@PostMapping("/books")
	@ResponseStatus(HttpStatus.CREATED)
	@PreAuthorize("hasRole('READER')")
	public AiRecommendationResponse recommendBooks(
		Principal principal,
		@Valid @RequestBody AiRecommendationRequest request
	) {
		return AiRecommendationMapper.toResponse(recommendationService.createRecommendations(principal.getName(), request));
	}

	@GetMapping("/my-requests")
	@PreAuthorize("hasRole('READER')")
	public List<AiRecommendationResponse> getMyRequests(Principal principal) {
		return recommendationService.getMyRequests(principal.getName())
			.stream()
			.map(AiRecommendationMapper::toResponse)
			.toList();
	}

	@GetMapping("/requests/{requestId}")
	@PreAuthorize("hasRole('READER')")
	public AiRecommendationResponse getRequest(Principal principal, @PathVariable UUID requestId) {
		return AiRecommendationMapper.toResponse(recommendationService.getRequest(principal.getName(), requestId));
	}
}
