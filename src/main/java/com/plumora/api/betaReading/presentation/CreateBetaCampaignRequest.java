package com.plumora.api.betaReading.presentation;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record CreateBetaCampaignRequest(
	@NotBlank @Size(max = 150) String title,
	@Size(max = 5000) String instructions,
	LocalDate deadline
) {
}
