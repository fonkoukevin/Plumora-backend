package com.plumora.api.shared.security;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Collections;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * Verifies Google Sign-In ID tokens sent by the app (mobile/web Google Sign-In SDK) directly
 * against Google's public keys - no server-to-server call using a client secret, and no
 * dependency on any Google API being reachable beyond the one-time (then cached) key fetch.
 */
@Service
public class GoogleIdTokenVerifierService {

	private static final Logger log = LoggerFactory.getLogger(GoogleIdTokenVerifierService.class);

	private final GoogleIdTokenVerifier verifier;

	public GoogleIdTokenVerifierService(@Value("${app.security.google.client-id:}") String clientId) {
		this.verifier = StringUtils.hasText(clientId)
			? new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), GsonFactory.getDefaultInstance())
				.setAudience(Collections.singletonList(clientId))
				.build()
			: null;
	}

	public boolean isConfigured() {
		return verifier != null;
	}

	public Optional<GoogleIdentity> verify(String idTokenString) {
		if (verifier == null || !StringUtils.hasText(idTokenString)) {
			return Optional.empty();
		}
		try {
			GoogleIdToken idToken = verifier.verify(idTokenString);
			if (idToken == null) {
				return Optional.empty();
			}
			GoogleIdToken.Payload payload = idToken.getPayload();
			if (!StringUtils.hasText(payload.getEmail()) || !Boolean.TRUE.equals(payload.getEmailVerified())) {
				return Optional.empty();
			}
			return Optional.of(new GoogleIdentity(
				payload.getEmail().toLowerCase(),
				(String) payload.get("given_name"),
				(String) payload.get("family_name"),
				(String) payload.get("picture")
			));
		} catch (GeneralSecurityException | IOException | IllegalArgumentException exception) {
			log.warn("Google ID token verification failed", exception);
			return Optional.empty();
		}
	}

	public record GoogleIdentity(String email, String firstName, String lastName, String pictureUrl) {
	}
}
