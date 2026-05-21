package com.plumora.api.reading.presentation;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import java.math.BigDecimal;
import java.util.UUID;

public record ReadingProgressRequest(
	UUID currentChapterId,
	@DecimalMin("0.00") @DecimalMax("100.00") @Digits(integer = 3, fraction = 2) BigDecimal progressPercentage
) {
}
