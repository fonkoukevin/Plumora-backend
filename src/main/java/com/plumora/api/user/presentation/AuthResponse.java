package com.plumora.api.user.presentation;

public record AuthResponse(
	String token,
	String tokenType,
	UserResponse user
) {
}
