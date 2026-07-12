package com.plumora.api.shared.exception;

public class AiInvalidResponseException extends ExternalServiceUnavailableException {
	public AiInvalidResponseException(String message) {
		super(message);
	}
}
