package com.plumora.api.user.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.plumora.api.shared.exception.BusinessException;
import com.plumora.api.user.domain.PasswordResetToken;
import com.plumora.api.user.domain.User;
import com.plumora.api.user.infrastructure.PasswordResetTokenRepository;
import com.plumora.api.user.infrastructure.UserRepository;
import com.plumora.api.user.infrastructure.mail.PasswordResetMailer;
import com.plumora.api.user.presentation.ForgotPasswordRequest;
import com.plumora.api.user.presentation.ResetPasswordRequest;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class PasswordResetServiceTest {

	private static final long EXPIRATION_MINUTES = 60;
	private static final String FRONTEND_BASE_URL = "https://app.plumora-books.fr";

	@Mock
	private UserRepository userRepository;

	@Mock
	private PasswordResetTokenRepository passwordResetTokenRepository;

	@Mock
	private PasswordResetMailer mailer;

	private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

	private PasswordResetService service;

	@BeforeEach
	void setUp() {
		service = new PasswordResetService(
			userRepository,
			passwordResetTokenRepository,
			passwordEncoder,
			mailer,
			EXPIRATION_MINUTES,
			FRONTEND_BASE_URL
		);
	}

	@Test
	void requestResetIssuesATokenAndEmailsTheLinkForAnExistingUser() {
		User user = user("reader@example.com");
		when(userRepository.findByEmail("reader@example.com")).thenReturn(Optional.of(user));
		when(passwordResetTokenRepository.findByUserAndUsedAtIsNull(user)).thenReturn(List.of());

		service.requestReset(new ForgotPasswordRequest("reader@example.com"));

		ArgumentCaptor<PasswordResetToken> tokenCaptor = ArgumentCaptor.forClass(PasswordResetToken.class);
		verify(passwordResetTokenRepository).save(tokenCaptor.capture());
		PasswordResetToken savedToken = tokenCaptor.getValue();
		assertThat(savedToken.getUser()).isEqualTo(user);
		assertThat(savedToken.getToken()).isNotBlank();
		assertThat(savedToken.getExpiresAt()).isAfter(LocalDateTime.now());

		ArgumentCaptor<String> linkCaptor = ArgumentCaptor.forClass(String.class);
		verify(mailer).sendResetLink(eq(user), linkCaptor.capture());
		assertThat(linkCaptor.getValue())
			.startsWith(FRONTEND_BASE_URL + "/reset-password?token=")
			.endsWith(savedToken.getToken());
	}

	@Test
	void requestResetDoesNothingButStillCompletesForAnUnknownEmail() {
		when(userRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

		service.requestReset(new ForgotPasswordRequest("unknown@example.com"));

		verify(passwordResetTokenRepository, never()).save(any(PasswordResetToken.class));
		verify(mailer, never()).sendResetLink(any(User.class), anyString());
	}

	@Test
	void requestResetInvalidatesPreviouslyIssuedUnusedTokens() {
		User user = user("reader@example.com");
		PasswordResetToken previousToken = token(user, "old-token", 30, null);
		when(userRepository.findByEmail("reader@example.com")).thenReturn(Optional.of(user));
		when(passwordResetTokenRepository.findByUserAndUsedAtIsNull(user)).thenReturn(List.of(previousToken));

		service.requestReset(new ForgotPasswordRequest("reader@example.com"));

		assertThat(previousToken.getUsedAt()).isNotNull();
		verify(passwordResetTokenRepository).saveAll(List.of(previousToken));
	}

	@Test
	void resetPasswordUpdatesThePasswordAndConsumesTheToken() {
		User user = user("reader@example.com");
		String originalHash = user.getPasswordHash();
		PasswordResetToken resetToken = token(user, "valid-token", 30, null);
		when(passwordResetTokenRepository.findByToken("valid-token")).thenReturn(Optional.of(resetToken));

		service.resetPassword(new ResetPasswordRequest("valid-token", "NewPassword123"));

		assertThat(user.getPasswordHash()).isNotEqualTo(originalHash);
		assertThat(passwordEncoder.matches("NewPassword123", user.getPasswordHash())).isTrue();
		assertThat(resetToken.getUsedAt()).isNotNull();
		verify(passwordResetTokenRepository, times(1)).save(resetToken);
	}

	@Test
	void resetPasswordRejectsAnUnknownToken() {
		when(passwordResetTokenRepository.findByToken("missing-token")).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.resetPassword(new ResetPasswordRequest("missing-token", "NewPassword123")))
			.isInstanceOf(BusinessException.class);
	}

	@Test
	void resetPasswordRejectsAnExpiredToken() {
		User user = user("reader@example.com");
		PasswordResetToken expiredToken = token(user, "expired-token", -10, null);
		when(passwordResetTokenRepository.findByToken("expired-token")).thenReturn(Optional.of(expiredToken));

		assertThatThrownBy(() -> service.resetPassword(new ResetPasswordRequest("expired-token", "NewPassword123")))
			.isInstanceOf(BusinessException.class);
	}

	@Test
	void resetPasswordRejectsAnAlreadyUsedToken() {
		User user = user("reader@example.com");
		PasswordResetToken usedToken = token(user, "used-token", 30, LocalDateTime.now().minusMinutes(5));
		when(passwordResetTokenRepository.findByToken("used-token")).thenReturn(Optional.of(usedToken));

		assertThatThrownBy(() -> service.resetPassword(new ResetPasswordRequest("used-token", "NewPassword123")))
			.isInstanceOf(BusinessException.class);
	}

	private User user(String email) {
		User user = new User();
		user.setId(UUID.randomUUID());
		user.setEmail(email);
		user.setUsername(email.substring(0, email.indexOf('@')));
		user.setPasswordHash(passwordEncoder.encode("OriginalPassword123"));
		return user;
	}

	private PasswordResetToken token(User user, String value, long minutesFromNowUntilExpiry, LocalDateTime usedAt) {
		PasswordResetToken token = new PasswordResetToken();
		token.setId(UUID.randomUUID());
		token.setUser(user);
		token.setToken(value);
		token.setExpiresAt(LocalDateTime.now().plusMinutes(minutesFromNowUntilExpiry));
		token.setUsedAt(usedAt);
		token.setCreatedAt(LocalDateTime.now());
		return token;
	}
}
