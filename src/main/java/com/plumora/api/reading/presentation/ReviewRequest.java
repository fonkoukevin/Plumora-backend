package com.plumora.api.reading.presentation;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ReviewRequest(
	@NotNull @Min(1) @Max(5) Integer rating,
	@Size(max = 5000) String comment
) {
}
