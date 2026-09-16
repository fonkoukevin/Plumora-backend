package com.plumora.api.user.application;

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
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EmailVerificationService {

	private final UserRepository userRepository;
	private final EmailVerificationTokenRepository emailVerificationTokenRepository;
	private final AccountVerificationMailer mailer;
	private final long tokenExpirationMinutes;
	private final String frontendBaseUrl;

	public EmailVerificationService(
		UserRepository userRepository,
		EmailVerificationTokenRepository emailVerificationTokenRepository,
		AccountVerificationMailer mailer,
		@Value("${app.account-verification.token-expiration-minutes}") long tokenExpirationMinutes,
		@Value("${app.account-verification.frontend-base-url}") String frontendBaseUrl
	) {
		this.userRepository = userRepository;
		this.emailVerificationTokenRepository = emailVerificationTokenRepository;
		this.mailer = mailer;
		this.tokenExpirationMinutes = tokenExpirationMinutes;
		this.frontendBaseUrl = frontendBaseUrl;
	}

	@Transactional
	public void sendVerificationEmail(User user) {
		issueTokenAndSendLink(user);
	}

	/**
	 * Always completes normally, whether or not an account exists for the given email (and silently
	 * no-ops for an already-verified one) - same anti-enumeration contract as
	 * PasswordResetService.requestReset, so the response never leaks account existence or
	 * verification state.
	 */
	@Transactional
	public void resendVerificationEmail(ResendVerificationRequest request) {
		userRepository.findByEmail(request.email().toLowerCase())
			.filter(user -> !user.isEmailVerified())
			.ifPresent(this::issueTokenAndSendLink);
	}

	private void issueTokenAndSendLink(User user) {
		invalidateExistingTokens(user);

		EmailVerificationToken verificationToken = new EmailVerificationToken();
		verificationToken.setUser(user);
		verificationToken.setToken(UUID.randomUUID().toString());
		verificationToken.setExpiresAt(LocalDateTime.now().plusMinutes(tokenExpirationMinutes));
		emailVerificationTokenRepository.save(verificationToken);

		String verificationLink = frontendBaseUrl + "/verify-email?token=" + verificationToken.getToken();
		mailer.sendVerificationLink(user, verificationLink);
	}

	private void invalidateExistingTokens(User user) {
		List<EmailVerificationToken> outstanding = emailVerificationTokenRepository.findByUserAndUsedAtIsNull(user);
		LocalDateTime now = LocalDateTime.now();
		outstanding.forEach(token -> token.setUsedAt(now));
		emailVerificationTokenRepository.saveAll(outstanding);
	}

	@Transactional
	public void verifyEmail(VerifyEmailRequest request) {
		EmailVerificationToken verificationToken = emailVerificationTokenRepository.findByToken(request.token())
			.filter(EmailVerificationToken::isUsable)
			.orElseThrow(() -> new BusinessException("This email verification link is invalid or has expired."));

		User user = verificationToken.getUser();
		user.setEmailVerified(true);
		userRepository.save(user);

		verificationToken.setUsedAt(LocalDateTime.now());
		emailVerificationTokenRepository.save(verificationToken);
	}
}
