package com.plumora.api.betaReading.presentation;

import com.plumora.api.betaReading.application.BetaReadingService;
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

@RestController
@RequestMapping
public class BetaCampaignController {

	private final BetaReadingService betaReadingService;

	public BetaCampaignController(BetaReadingService betaReadingService) {
		this.betaReadingService = betaReadingService;
	}

	@PostMapping("/books/{bookId}/beta-campaigns")
	@ResponseStatus(HttpStatus.CREATED)
	@PreAuthorize("hasRole('AUTHOR')")
	public BetaCampaignResponse createCampaign(
		Principal principal,
		@PathVariable UUID bookId,
		@Valid @RequestBody CreateBetaCampaignRequest request
	) {
		return BetaReadingMapper.toCampaignResponse(betaReadingService.createCampaign(principal.getName(), bookId, request));
	}

	@GetMapping("/books/{bookId}/beta-campaigns")
	@PreAuthorize("hasRole('AUTHOR')")
	public List<BetaCampaignResponse> getBookCampaigns(Principal principal, @PathVariable UUID bookId) {
		return betaReadingService.getBookCampaigns(principal.getName(), bookId)
			.stream()
			.map(BetaReadingMapper::toCampaignResponse)
			.toList();
	}

	@GetMapping("/beta-campaigns")
	@PreAuthorize("hasRole('BETA_READER')")
	public List<BetaCampaignResponse> getOpenCampaigns(Principal principal) {
		return betaReadingService.getOpenCampaigns(principal.getName())
			.stream()
			.map(BetaReadingMapper::toCampaignResponse)
			.toList();
	}

	@GetMapping("/beta-campaigns/{campaignId}")
	@PreAuthorize("hasAnyRole('AUTHOR', 'BETA_READER')")
	public BetaCampaignResponse getCampaign(Principal principal, @PathVariable UUID campaignId) {
		return BetaReadingMapper.toCampaignResponse(betaReadingService.getCampaign(principal.getName(), campaignId));
	}

	@PatchMapping("/beta-campaigns/{campaignId}/close")
	@PreAuthorize("hasRole('AUTHOR')")
	public BetaCampaignResponse closeCampaign(Principal principal, @PathVariable UUID campaignId) {
		return BetaReadingMapper.toCampaignResponse(betaReadingService.closeCampaign(principal.getName(), campaignId));
	}

	@PatchMapping("/beta-campaigns/{campaignId}/cancel")
	@PreAuthorize("hasRole('AUTHOR')")
	public BetaCampaignResponse cancelCampaign(Principal principal, @PathVariable UUID campaignId) {
		return BetaReadingMapper.toCampaignResponse(betaReadingService.cancelCampaign(principal.getName(), campaignId));
	}
}
