package com.plumora.api.user.application;

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
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PasswordResetService {

	private final UserRepository userRepository;
	private final PasswordResetTokenRepository passwordResetTokenRepository;
	private final PasswordEncoder passwordEncoder;
	private final PasswordResetMailer mailer;
	private final long tokenExpirationMinutes;
	private final String frontendBaseUrl;

	public PasswordResetService(
		UserRepository userRepository,
		PasswordResetTokenRepository passwordResetTokenRepository,
		PasswordEncoder passwordEncoder,
		PasswordResetMailer mailer,
		@Value("${app.password-reset.token-expiration-minutes}") long tokenExpirationMinutes,
		@Value("${app.password-reset.frontend-base-url}") String frontendBaseUrl
	) {
		this.userRepository = userRepository;
		this.passwordResetTokenRepository = passwordResetTokenRepository;
		this.passwordEncoder = passwordEncoder;
		this.mailer = mailer;
		this.tokenExpirationMinutes = tokenExpirationMinutes;
		this.frontendBaseUrl = frontendBaseUrl;
	}

	/**
	 * Always completes normally, whether or not an account exists for the given email - the
	 * response must never be used to leak account existence (see docs/api-contract.md, frontend
	 * repository). Google-only accounts are not special-cased: completing a reset simply gives
	 * them a real password in addition to Google Sign-In, which avoids a second, distinguishable
	 * response path for that case.
	 */
	@Transactional
	public void requestReset(ForgotPasswordRequest request) {
		userRepository.findByEmail(request.email().toLowerCase())
			.ifPresent(this::issueTokenAndSendLink);
	}

	private void issueTokenAndSendLink(User user) {
		invalidateExistingTokens(user);

		PasswordResetToken resetToken = new PasswordResetToken();
		resetToken.setUser(user);
		resetToken.setToken(UUID.randomUUID().toString());
		resetToken.setExpiresAt(LocalDateTime.now().plusMinutes(tokenExpirationMinutes));
		passwordResetTokenRepository.save(resetToken);

		String resetLink = frontendBaseUrl + "/reset-password?token=" + resetToken.getToken();
		mailer.sendResetLink(user, resetLink);
	}

	private void invalidateExistingTokens(User user) {
		List<PasswordResetToken> outstanding = passwordResetTokenRepository.findByUserAndUsedAtIsNull(user);
		LocalDateTime now = LocalDateTime.now();
		outstanding.forEach(token -> token.setUsedAt(now));
		passwordResetTokenRepository.saveAll(outstanding);
	}

	@Transactional
	public void resetPassword(ResetPasswordRequest request) {
		PasswordResetToken resetToken = passwordResetTokenRepository.findByToken(request.token())
			.filter(PasswordResetToken::isUsable)
			.orElseThrow(() -> new BusinessException("This password reset link is invalid or has expired."));

		User user = resetToken.getUser();
		user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
		userRepository.save(user);

		resetToken.setUsedAt(LocalDateTime.now());
		passwordResetTokenRepository.save(resetToken);
	}
}
