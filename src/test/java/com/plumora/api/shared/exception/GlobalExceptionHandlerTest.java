package com.plumora.api.shared.exception;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;

class GlobalExceptionHandlerTest {

	private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

	@Test
	void handleForbiddenReturns403ForAccessDeniedException() {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/books/book-id/beta-campaigns");

		ResponseEntity<ErrorResponse> response = handler.handleForbidden(
			new AccessDeniedException("Access Denied"),
			request
		);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
		assertThat(response.getBody()).isNotNull();
		assertThat(response.getBody().status()).isEqualTo(403);
		assertThat(response.getBody().error()).isEqualTo("Forbidden");
		assertThat(response.getBody().message()).isEqualTo("Access Denied");
		assertThat(response.getBody().path()).isEqualTo("/api/v1/books/book-id/beta-campaigns");
	}

	@Test
	void handleForbiddenReturns403ForUnauthorizedActionException() {
		MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/beta-campaigns/campaign-id/invitations");

		ResponseEntity<ErrorResponse> response = handler.handleForbidden(
			new UnauthorizedActionException("Only the book author can manage this beta-reading campaign"),
			request
		);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
		assertThat(response.getBody()).isNotNull();
		assertThat(response.getBody().status()).isEqualTo(403);
		assertThat(response.getBody().error()).isEqualTo("Forbidden");
		assertThat(response.getBody().message()).isEqualTo("Only the book author can manage this beta-reading campaign");
		assertThat(response.getBody().path()).isEqualTo("/api/v1/beta-campaigns/campaign-id/invitations");
	}

	@Test
	void handleUnauthorizedReturns401ForBadCredentialsException() {
		MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/auth/login");

		ResponseEntity<ErrorResponse> response = handler.handleUnauthorized(
			new BadCredentialsException("Bad credentials"),
			request
		);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
		assertThat(response.getBody()).isNotNull();
		assertThat(response.getBody().status()).isEqualTo(401);
		assertThat(response.getBody().error()).isEqualTo("Unauthorized");
		assertThat(response.getBody().message()).isEqualTo("Bad credentials");
		assertThat(response.getBody().path()).isEqualTo("/api/v1/auth/login");
	}
}
