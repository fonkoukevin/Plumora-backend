package com.plumora.api.admin.presentation;

import com.plumora.api.book.domain.BookStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateBookStatusRequest(
	@NotNull BookStatus status,
	@Size(max = 500) String reason
) {
}
