package com.plumora.api.user.infrastructure.mail;

import com.plumora.api.user.domain.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Default mailer: no SMTP/email provider is configured for this project yet, so the reset link is
 * written to the application log instead of being emailed. This keeps the forgot-password flow
 * fully functional end-to-end (token issued, validated, password changed) without inventing
 * credentials for a mail provider that was never set up. Swap in a real provider (SMTP, SendGrid,
 * etc.) behind the same PasswordResetMailer interface, selected via plumora.mail.provider, once
 * one is chosen - see docs/bloc2/password-reset-implementation.md.
 */
@Component
@ConditionalOnProperty(name = "plumora.mail.provider", havingValue = "log", matchIfMissing = true)
public class LoggingPasswordResetMailer implements PasswordResetMailer {

	private static final Logger log = LoggerFactory.getLogger(LoggingPasswordResetMailer.class);

	@Override
	public void sendResetLink(User user, String resetLink) {
		log.info("Password reset requested for {} - reset link (no mail provider configured): {}", user.getEmail(), resetLink);
	}
}
