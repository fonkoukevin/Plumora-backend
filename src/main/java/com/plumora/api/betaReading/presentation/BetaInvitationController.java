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
public class BetaInvitationController {

	private final BetaReadingService betaReadingService;

	public BetaInvitationController(BetaReadingService betaReadingService) {
		this.betaReadingService = betaReadingService;
	}

	@PostMapping("/beta-campaigns/{campaignId}/invitations")
	@ResponseStatus(HttpStatus.CREATED)
	@PreAuthorize("hasRole('AUTHOR')")
	public BetaInvitationResponse createInvitation(
		Principal principal,
		@PathVariable UUID campaignId,
		@Valid @RequestBody CreateBetaInvitationRequest request
	) {
		return BetaReadingMapper.toInvitationResponse(betaReadingService.createInvitation(principal.getName(), campaignId, request));
	}

	@GetMapping("/beta-campaigns/{campaignId}/invitations")
	@PreAuthorize("hasRole('AUTHOR')")
	public List<BetaInvitationResponse> getCampaignInvitations(Principal principal, @PathVariable UUID campaignId) {
		return betaReadingService.getCampaignInvitations(principal.getName(), campaignId)
			.stream()
			.map(BetaReadingMapper::toInvitationResponse)
			.toList();
	}

	@GetMapping("/beta-invitations/my-invitations")
	@PreAuthorize("hasRole('BETA_READER')")
	public List<BetaInvitationResponse> getMyInvitations(Principal principal) {
		return betaReadingService.getMyInvitations(principal.getName())
			.stream()
			.map(BetaReadingMapper::toInvitationResponse)
			.toList();
	}

	@PatchMapping("/beta-invitations/{invitationId}/accept")
	@PreAuthorize("hasRole('BETA_READER')")
	public BetaInvitationResponse acceptInvitation(Principal principal, @PathVariable UUID invitationId) {
		return BetaReadingMapper.toInvitationResponse(betaReadingService.acceptInvitation(principal.getName(), invitationId));
	}

	@PatchMapping("/beta-invitations/{invitationId}/refuse")
	@PreAuthorize("hasRole('BETA_READER')")
	public BetaInvitationResponse refuseInvitation(Principal principal, @PathVariable UUID invitationId) {
		return BetaReadingMapper.toInvitationResponse(betaReadingService.refuseInvitation(principal.getName(), invitationId));
	}
}
