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
import com.plumora.api.user.domain.EmailVerificationToken;
import com.plumora.api.user.domain.User;
import com.plumora.api.user.infrastructure.EmailVerificationTokenRepository;
import com.plumora.api.user.infrastructure.UserRepository;
import com.plumora.api.user.infrastructure.mail.AccountVerificationMailer;
import com.plumora.api.user.presentation.ResendVerificationRequest;
import com.plumora.api.user.presentation.VerifyEmailRequest;
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

@ExtendWith(MockitoExtension.class)
class EmailVerificationServiceTest {

	private static final long EXPIRATION_MINUTES = 1440;
	private static final String FRONTEND_BASE_URL = "https://app.plumora-books.fr";

	@Mock
	private UserRepository userRepository;

	@Mock
	private EmailVerificationTokenRepository emailVerificationTokenRepository;

	@Mock
	private AccountVerificationMailer mailer;

	private EmailVerificationService service;

	@BeforeEach
	void setUp() {
		service = new EmailVerificationService(
			userRepository,
			emailVerificationTokenRepository,
			mailer,
			EXPIRATION_MINUTES,
			FRONTEND_BASE_URL
		);
	}

	@Test
	void sendVerificationEmailIssuesATokenAndEmailsTheLink() {
		User user = user("reader@example.com");
		when(emailVerificationTokenRepository.findByUserAndUsedAtIsNull(user)).thenReturn(List.of());

		service.sendVerificationEmail(user);

		ArgumentCaptor<EmailVerificationToken> tokenCaptor = ArgumentCaptor.forClass(EmailVerificationToken.class);
		verify(emailVerificationTokenRepository).save(tokenCaptor.capture());
		EmailVerificationToken savedToken = tokenCaptor.getValue();
		assertThat(savedToken.getUser()).isEqualTo(user);
		assertThat(savedToken.getToken()).isNotBlank();
		assertThat(savedToken.getExpiresAt()).isAfter(LocalDateTime.now());

		ArgumentCaptor<String> linkCaptor = ArgumentCaptor.forClass(String.class);
		verify(mailer).sendVerificationLink(eq(user), linkCaptor.capture());
		assertThat(linkCaptor.getValue())
			.startsWith(FRONTEND_BASE_URL + "/verify-email?token=")
			.endsWith(savedToken.getToken());
	}

	@Test
	void resendVerificationEmailIssuesATokenForAnUnverifiedExistingUser() {
		User user = user("reader@example.com");
		when(userRepository.findByEmail("reader@example.com")).thenReturn(Optional.of(user));
		when(emailVerificationTokenRepository.findByUserAndUsedAtIsNull(user)).thenReturn(List.of());

		service.resendVerificationEmail(new ResendVerificationRequest("reader@example.com"));

		verify(emailVerificationTokenRepository).save(any(EmailVerificationToken.class));
		verify(mailer).sendVerificationLink(eq(user), anyString());
	}

	@Test
	void resendVerificationEmailDoesNothingButStillCompletesForAnUnknownEmail() {
		when(userRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

		service.resendVerificationEmail(new ResendVerificationRequest("unknown@example.com"));

		verify(emailVerificationTokenRepository, never()).save(any(EmailVerificationToken.class));
		verify(mailer, never()).sendVerificationLink(any(User.class), anyString());
	}

	@Test
	void resendVerificationEmailDoesNothingButStillCompletesForAnAlreadyVerifiedUser() {
		User user = user("reader@example.com");
		user.setEmailVerified(true);
		when(userRepository.findByEmail("reader@example.com")).thenReturn(Optional.of(user));

		service.resendVerificationEmail(new ResendVerificationRequest("reader@example.com"));

		verify(emailVerificationTokenRepository, never()).save(any(EmailVerificationToken.class));
		verify(mailer, never()).sendVerificationLink(any(User.class), anyString());
	}

	@Test
	void sendVerificationEmailInvalidatesPreviouslyIssuedUnusedTokens() {
		User user = user("reader@example.com");
		EmailVerificationToken previousToken = token(user, "old-token", 30, null);
		when(emailVerificationTokenRepository.findByUserAndUsedAtIsNull(user)).thenReturn(List.of(previousToken));

		service.sendVerificationEmail(user);

		assertThat(previousToken.getUsedAt()).isNotNull();
		verify(emailVerificationTokenRepository).saveAll(List.of(previousToken));
	}

	@Test
	void verifyEmailActivatesTheAccountAndConsumesTheToken() {
		User user = user("reader@example.com");
		assertThat(user.isEmailVerified()).isFalse();
		EmailVerificationToken verificationToken = token(user, "valid-token", 30, null);
		when(emailVerificationTokenRepository.findByToken("valid-token")).thenReturn(Optional.of(verificationToken));

		service.verifyEmail(new VerifyEmailRequest("valid-token"));

		assertThat(user.isEmailVerified()).isTrue();
		assertThat(verificationToken.getUsedAt()).isNotNull();
		verify(userRepository).save(user);
		verify(emailVerificationTokenRepository, times(1)).save(verificationToken);
	}

	@Test
	void verifyEmailRejectsAnUnknownToken() {
		when(emailVerificationTokenRepository.findByToken("missing-token")).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.verifyEmail(new VerifyEmailRequest("missing-token")))
			.isInstanceOf(BusinessException.class);
	}

	@Test
	void verifyEmailRejectsAnExpiredToken() {
		User user = user("reader@example.com");
		EmailVerificationToken expiredToken = token(user, "expired-token", -10, null);
		when(emailVerificationTokenRepository.findByToken("expired-token")).thenReturn(Optional.of(expiredToken));

		assertThatThrownBy(() -> service.verifyEmail(new VerifyEmailRequest("expired-token")))
			.isInstanceOf(BusinessException.class);
	}

	@Test
	void verifyEmailRejectsAnAlreadyUsedToken() {
		User user = user("reader@example.com");
		EmailVerificationToken usedToken = token(user, "used-token", 30, LocalDateTime.now().minusMinutes(5));
		when(emailVerificationTokenRepository.findByToken("used-token")).thenReturn(Optional.of(usedToken));

		assertThatThrownBy(() -> service.verifyEmail(new VerifyEmailRequest("used-token")))
			.isInstanceOf(BusinessException.class);
	}

	private User user(String email) {
		User user = new User();
		user.setId(UUID.randomUUID());
		user.setEmail(email);
		user.setUsername(email.substring(0, email.indexOf('@')));
		return user;
	}

	private EmailVerificationToken token(User user, String value, long minutesFromNowUntilExpiry, LocalDateTime usedAt) {
		EmailVerificationToken token = new EmailVerificationToken();
		token.setId(UUID.randomUUID());
		token.setUser(user);
		token.setToken(value);
		token.setExpiresAt(LocalDateTime.now().plusMinutes(minutesFromNowUntilExpiry));
		token.setUsedAt(usedAt);
		token.setCreatedAt(LocalDateTime.now());
		return token;
	}
}
