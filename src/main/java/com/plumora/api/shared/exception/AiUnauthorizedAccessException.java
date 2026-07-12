package com.plumora.api.shared.exception;

public class AiUnauthorizedAccessException extends UnauthorizedActionException {
	public AiUnauthorizedAccessException(String message) {
		super(message);
	}
}
