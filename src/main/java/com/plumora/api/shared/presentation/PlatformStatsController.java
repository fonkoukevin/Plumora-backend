package com.plumora.api.shared.presentation;

import com.plumora.api.shared.application.PlatformStatsService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/stats")
public class PlatformStatsController {

	private final PlatformStatsService platformStatsService;

	public PlatformStatsController(PlatformStatsService platformStatsService) {
		this.platformStatsService = platformStatsService;
	}

	@GetMapping("/platform")
	public PlatformStatsResponse getPlatformStats() {
		return platformStatsService.getPublicStats();
	}
}
