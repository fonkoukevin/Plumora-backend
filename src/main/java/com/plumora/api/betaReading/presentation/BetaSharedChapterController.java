package com.plumora.api.betaReading.presentation;

import com.plumora.api.betaReading.application.BetaReadingService;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.List;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping
public class BetaSharedChapterController {

	private final BetaReadingService betaReadingService;

	public BetaSharedChapterController(BetaReadingService betaReadingService) {
		this.betaReadingService = betaReadingService;
	}

	@GetMapping("/beta-campaigns/{campaignId}/chapters")
	@PreAuthorize("hasAnyRole('AUTHOR', 'BETA_READER')")
	public List<BetaSharedChapterResponse> getSharedChapters(Principal principal, @PathVariable UUID campaignId) {
		return betaReadingService.getSharedChapters(principal.getName(), campaignId)
			.stream()
			.map(BetaReadingMapper::toSharedChapterResponse)
			.toList();
	}

	@PutMapping("/beta-campaigns/{campaignId}/chapters")
	@PreAuthorize("hasRole('AUTHOR')")
	public List<BetaSharedChapterResponse> updateSharedChapters(
		Principal principal,
		@PathVariable UUID campaignId,
		@Valid @RequestBody UpdateSharedChaptersRequest request
	) {
		return betaReadingService.updateSharedChapters(principal.getName(), campaignId, request)
			.stream()
			.map(BetaReadingMapper::toSharedChapterResponse)
			.toList();
	}
}
