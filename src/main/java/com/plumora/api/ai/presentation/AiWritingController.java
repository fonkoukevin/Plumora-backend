package com.plumora.api.ai.presentation;

import com.plumora.api.ai.application.AiWritingService;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Also exposes the stateless "Plumo IA" writing-assistant endpoints (rewrite/summarize/continue/titles),
 * which do not persist requests or suggestions, unlike the accept/modify/ignore suggestion workflow above.
 */
@RestController
@RequestMapping("/ai/writing")
public class AiWritingController {

	private final AiWritingService aiWritingService;

	public AiWritingController(AiWritingService aiWritingService) {
		this.aiWritingService = aiWritingService;
	}

	@PostMapping("/suggestions")
	@ResponseStatus(HttpStatus.CREATED)
	@PreAuthorize("hasRole('AUTHOR')")
	public AiWritingSuggestionResponse createSuggestion(
		Principal principal,
		@Valid @RequestBody CreateAiWritingSuggestionRequest request
	) {
		return AiWritingMapper.toSuggestionResponse(aiWritingService.createSuggestion(principal.getName(), request));
	}

	@GetMapping("/requests")
	@PreAuthorize("hasRole('AUTHOR')")
	public List<AiWritingRequestResponse> getMyRequests(Principal principal) {
		return aiWritingService.getMyRequests(principal.getName())
			.stream()
			.map(request -> AiWritingMapper.toRequestResponse(request, List.of()))
			.toList();
	}

	@GetMapping("/requests/{requestId}")
	@PreAuthorize("hasRole('AUTHOR')")
	public AiWritingRequestResponse getRequest(Principal principal, @PathVariable UUID requestId) {
		var request = aiWritingService.getRequest(principal.getName(), requestId);
		var suggestions = aiWritingService.getRequestSuggestions(principal.getName(), request);
		return AiWritingMapper.toRequestResponse(request, suggestions);
	}

	@PatchMapping("/suggestions/{suggestionId}/accept")
	@PreAuthorize("hasRole('AUTHOR')")
	public AiWritingSuggestionResponse acceptSuggestion(Principal principal, @PathVariable UUID suggestionId) {
		return AiWritingMapper.toSuggestionResponse(aiWritingService.acceptSuggestion(principal.getName(), suggestionId));
	}

	@PatchMapping("/suggestions/{suggestionId}/modify")
	@PreAuthorize("hasRole('AUTHOR')")
	public AiWritingSuggestionResponse modifySuggestion(Principal principal, @PathVariable UUID suggestionId) {
		return AiWritingMapper.toSuggestionResponse(aiWritingService.modifySuggestion(principal.getName(), suggestionId));
	}

	@PatchMapping("/suggestions/{suggestionId}/ignore")
	@PreAuthorize("hasRole('AUTHOR')")
	public AiWritingSuggestionResponse ignoreSuggestion(Principal principal, @PathVariable UUID suggestionId) {
		return AiWritingMapper.toSuggestionResponse(aiWritingService.ignoreSuggestion(principal.getName(), suggestionId));
	}

	@PostMapping("/rewrite")
	@PreAuthorize("hasRole('AUTHOR')")
	public AiTextGenerationResponse rewrite(Principal principal, @Valid @RequestBody AiTextGenerationRequest request) {
		return aiWritingService.rewrite(principal.getName(), request);
	}

	@PostMapping("/summarize")
	@PreAuthorize("hasRole('AUTHOR')")
	public AiTextGenerationResponse summarize(Principal principal, @Valid @RequestBody AiTextGenerationRequest request) {
		return aiWritingService.summarize(principal.getName(), request);
	}

	@PostMapping("/continue")
	@PreAuthorize("hasRole('AUTHOR')")
	public AiTextGenerationResponse continueWriting(Principal principal, @Valid @RequestBody AiTextGenerationRequest request) {
		return aiWritingService.continueWriting(principal.getName(), request);
	}

	@PostMapping("/titles")
	@PreAuthorize("hasRole('AUTHOR')")
	public AiTitleSuggestionResponse suggestTitles(Principal principal, @Valid @RequestBody AiTextGenerationRequest request) {
		return aiWritingService.suggestTitles(principal.getName(), request);
	}
}
