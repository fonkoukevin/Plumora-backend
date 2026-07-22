package com.plumora.api.shared.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class GoogleIdTokenVerifierServiceTest {

	@Test
	void isConfiguredReturnsFalseWithoutAClientId() {
		GoogleIdTokenVerifierService service = new GoogleIdTokenVerifierService("");

		assertThat(service.isConfigured()).isFalse();
	}

	@Test
	void isConfiguredReturnsTrueWithAClientId() {
		GoogleIdTokenVerifierService service = new GoogleIdTokenVerifierService("test-client-id.apps.googleusercontent.com");

		assertThat(service.isConfigured()).isTrue();
	}

	@Test
	void verifyReturnsEmptyWithoutMakingAnyCallWhenNotConfigured() {
		GoogleIdTokenVerifierService service = new GoogleIdTokenVerifierService("");

		assertThat(service.verify("any-token")).isEmpty();
	}

	@Test
	void verifyReturnsEmptyForABlankToken() {
		GoogleIdTokenVerifierService service = new GoogleIdTokenVerifierService("test-client-id.apps.googleusercontent.com");

		assertThat(service.verify("  ")).isEmpty();
		assertThat(service.verify(null)).isEmpty();
	}

	@Test
	void verifyReturnsEmptyForAStructurallyInvalidToken() {
		GoogleIdTokenVerifierService service = new GoogleIdTokenVerifierService("test-client-id.apps.googleusercontent.com");

		assertThat(service.verify("this-is-not-a-jwt")).isEmpty();
	}
}
