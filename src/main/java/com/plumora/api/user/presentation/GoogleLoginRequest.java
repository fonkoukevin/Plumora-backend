package com.plumora.api.user.presentation;

import jakarta.validation.constraints.NotBlank;

public record GoogleLoginRequest(
	@NotBlank String idToken
) {
}
