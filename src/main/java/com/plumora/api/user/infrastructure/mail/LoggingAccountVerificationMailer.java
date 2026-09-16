package com.plumora.api.user.infrastructure.mail;

import com.plumora.api.user.domain.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Default mailer: same rationale as {@link LoggingPasswordResetMailer} - no SMTP/email provider is
 * configured for this project yet, so the verification link is written to the application log
 * instead of being emailed. This keeps the registration flow fully functional end-to-end (token
 * issued, validated, account activated) without inventing credentials for a mail provider that was
 * never set up. Swap in a real provider behind the same AccountVerificationMailer interface,
 * selected via plumora.mail.provider, once one is chosen.
 */
@Component
@ConditionalOnProperty(name = "plumora.mail.provider", havingValue = "log", matchIfMissing = true)
public class LoggingAccountVerificationMailer implements AccountVerificationMailer {

	private static final Logger log = LoggerFactory.getLogger(LoggingAccountVerificationMailer.class);

	@Override
	public void sendVerificationLink(User user, String verificationLink) {
		log.info(
			"Account verification requested for {} - verification link (no mail provider configured): {}",
			user.getEmail(),
			verificationLink
		);
	}
}
