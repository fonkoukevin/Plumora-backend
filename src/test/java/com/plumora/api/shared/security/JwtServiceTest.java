package com.plumora.api.shared.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.plumora.api.user.domain.Role;
import com.plumora.api.user.domain.RoleName;
import com.plumora.api.user.domain.User;
import io.jsonwebtoken.JwtException;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;

@ExtendWith(MockitoExtension.class)
class JwtServiceTest {

	private static final long ONE_HOUR_MILLIS = 3_600_000L;
	private static final String SECRET = "unit-test-jwt-secret-with-more-than-32-characters";

	@Mock
	private UserDetails userDetails;

	@Test
	void generatedTokenRoundTripsToTheUsersEmail() {
		JwtService jwtService = new JwtService(SECRET, ONE_HOUR_MILLIS);

		String token = jwtService.generateToken(userWithEmail("reader@example.com"));

		assertThat(jwtService.extractUsername(token)).isEqualTo("reader@example.com");
	}

	@Test
	void isTokenValidReturnsTrueForTheMatchingUserAndAnUnexpiredToken() {
		JwtService jwtService = new JwtService(SECRET, ONE_HOUR_MILLIS);
		String token = jwtService.generateToken(userWithEmail("reader@example.com"));
		when(userDetails.getUsername()).thenReturn("reader@example.com");

		assertThat(jwtService.isTokenValid(token, userDetails)).isTrue();
	}

	@Test
	void isTokenValidReturnsFalseWhenTheUsernameDoesNotMatch() {
		JwtService jwtService = new JwtService(SECRET, ONE_HOUR_MILLIS);
		String token = jwtService.generateToken(userWithEmail("reader@example.com"));
		when(userDetails.getUsername()).thenReturn("someone-else@example.com");

		assertThat(jwtService.isTokenValid(token, userDetails)).isFalse();
	}

	@Test
	void anExpiredTokenIsRejectedRatherThanAcceptedSilently() {
		JwtService jwtService = new JwtService(SECRET, -1_000L);
		String expiredToken = jwtService.generateToken(userWithEmail("reader@example.com"));

		assertThatThrownBy(() -> jwtService.isTokenValid(expiredToken, userDetails))
			.isInstanceOf(JwtException.class);
	}

	@Test
	void aTokenSignedWithADifferentSecretIsRejected() {
		JwtService issuer = new JwtService(SECRET, ONE_HOUR_MILLIS);
		JwtService verifier = new JwtService("a-completely-different-unit-test-secret-value-0000", ONE_HOUR_MILLIS);
		String token = issuer.generateToken(userWithEmail("reader@example.com"));

		assertThatThrownBy(() -> verifier.extractUsername(token))
			.isInstanceOf(JwtException.class);
	}

	@Test
	void aTamperedTokenIsRejected() {
		JwtService jwtService = new JwtService(SECRET, ONE_HOUR_MILLIS);
		String token = jwtService.generateToken(userWithEmail("reader@example.com"));
		String tampered = token.substring(0, token.length() - 1) + (token.endsWith("A") ? "B" : "A");

		assertThatThrownBy(() -> jwtService.extractUsername(tampered))
			.isInstanceOf(JwtException.class);
	}

	@Test
	void acceptsAShortSecretForLocalDevelopmentConvenience() {
		JwtService jwtService = new JwtService("short-dev-secret", ONE_HOUR_MILLIS);

		String token = jwtService.generateToken(userWithEmail("reader@example.com"));

		assertThat(jwtService.extractUsername(token)).isEqualTo("reader@example.com");
	}

	private static User userWithEmail(String email) {
		User user = new User();
		user.setId(UUID.randomUUID());
		user.setFirstname("Test");
		user.setLastname("User");
		user.setUsername("testuser");
		user.setEmail(email);
		Role role = new Role(RoleName.READER, "Reader");
		role.setId(UUID.randomUUID());
		user.setRoles(Set.of(role));
		return user;
	}
}
