package com.plumora.api.betaReading.presentation;

import com.plumora.api.betaReading.application.BetaCommentService;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
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
public class BetaCommentController {

	private final BetaCommentService betaCommentService;

	public BetaCommentController(BetaCommentService betaCommentService) {
		this.betaCommentService = betaCommentService;
	}

	@PostMapping("/beta-comments")
	@ResponseStatus(HttpStatus.CREATED)
	@PreAuthorize("hasRole('BETA_READER')")
	public BetaCommentResponse createComment(
		Principal principal,
		@Valid @RequestBody CreateBetaCommentRequest request
	) {
		return BetaReadingMapper.toCommentResponse(betaCommentService.createComment(principal.getName(), request));
	}

	@GetMapping("/beta-campaigns/{campaignId}/comments")
	@PreAuthorize("hasAnyRole('AUTHOR', 'BETA_READER')")
	public List<BetaCommentResponse> getCampaignComments(Principal principal, @PathVariable UUID campaignId) {
		return betaCommentService.getCampaignComments(principal.getName(), campaignId)
			.stream()
			.map(BetaReadingMapper::toCommentResponse)
			.toList();
	}

	@GetMapping("/books/{bookId}/beta-comments")
	@PreAuthorize("hasRole('AUTHOR')")
	public List<BetaCommentResponse> getBookComments(Principal principal, @PathVariable UUID bookId) {
		return betaCommentService.getBookComments(principal.getName(), bookId)
			.stream()
			.map(BetaReadingMapper::toCommentResponse)
			.toList();
	}

	@GetMapping("/chapters/{chapterId}/beta-comments")
	@PreAuthorize("hasAnyRole('AUTHOR', 'BETA_READER')")
	public List<BetaCommentResponse> getChapterComments(Principal principal, @PathVariable UUID chapterId) {
		return betaCommentService.getChapterComments(principal.getName(), chapterId)
			.stream()
			.map(BetaReadingMapper::toCommentResponse)
			.toList();
	}

	@PatchMapping("/beta-comments/{commentId}/status")
	@PreAuthorize("hasRole('AUTHOR')")
	public BetaCommentResponse updateCommentStatus(
		Principal principal,
		@PathVariable UUID commentId,
		@Valid @RequestBody UpdateBetaCommentStatusRequest request
	) {
		return BetaReadingMapper.toCommentResponse(
			betaCommentService.updateCommentStatus(principal.getName(), commentId, request.status())
		);
	}

	@DeleteMapping("/beta-comments/{commentId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@PreAuthorize("hasRole('BETA_READER')")
	public void deleteComment(Principal principal, @PathVariable UUID commentId) {
		betaCommentService.deleteComment(principal.getName(), commentId);
	}
}
