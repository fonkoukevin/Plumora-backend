package com.plumora.api.ai.presentation;

import com.plumora.api.ai.application.AiBetaReadingAnalysisService;
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
@RequestMapping("/ai/beta-reading")
public class AiBetaReadingAnalysisController {

	private final AiBetaReadingAnalysisService analysisService;

	public AiBetaReadingAnalysisController(AiBetaReadingAnalysisService analysisService) {
		this.analysisService = analysisService;
	}

	@PostMapping("/analyze")
	@ResponseStatus(HttpStatus.OK)
	@PreAuthorize("hasRole('AUTHOR')")
	public AiBetaReadingAnalysisResponse analyze(
		Principal principal,
		@Valid @RequestBody AiBetaReadingAnalysisRequest request
	) {
		return analysisService.analyze(principal.getName(), request);
	}
}
