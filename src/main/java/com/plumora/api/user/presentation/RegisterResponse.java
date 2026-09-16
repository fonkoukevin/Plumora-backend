package com.plumora.api.user.presentation;

public record RegisterResponse(
	String message,
	UserResponse user
) {
}
