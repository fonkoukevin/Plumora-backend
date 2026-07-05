package com.plumora.api.shared.exception;

public class ExternalServiceUnavailableException extends RuntimeException {
	public ExternalServiceUnavailableException(String message) {
		super(message);
	}
}
