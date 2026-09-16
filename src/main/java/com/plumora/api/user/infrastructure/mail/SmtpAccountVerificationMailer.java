package com.plumora.api.user.infrastructure.mail;

import com.plumora.api.user.domain.User;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@ConditionalOnProperty(name = "plumora.mail.provider", havingValue = "smtp")
public class SmtpAccountVerificationMailer implements AccountVerificationMailer {

	private static final Logger log = LoggerFactory.getLogger(SmtpAccountVerificationMailer.class);

	private final JavaMailSender mailSender;
	private final String fromAddress;

	public SmtpAccountVerificationMailer(
		JavaMailSender mailSender,
		// See SmtpPasswordResetMailer's constructor comment: MAIL_FROM can be set but empty, which
		// Spring's ${a:${b:default}} placeholder syntax does not treat as absent - resolved here
		// instead, where StringUtils.hasText can tell "blank" from "absent" correctly.
		@Value("${plumora.mail.from-address:}") String configuredFromAddress,
		@Value("${spring.mail.username:}") String smtpUsername
	) {
		this.mailSender = mailSender;
		this.fromAddress = StringUtils.hasText(configuredFromAddress) ? configuredFromAddress : smtpUsername;
	}

	@Override
	public void sendVerificationLink(User user, String verificationLink) {
		try {
			MimeMessage message = mailSender.createMimeMessage();
			MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");
			helper.setFrom(fromAddress);
			helper.setTo(user.getEmail());
			helper.setSubject("Confirmez votre compte Plumora");
			helper.setText(
				"Bonjour " + user.getFirstname() + ",\n\n"
					+ "Bienvenue sur Plumora ! Merci de confirmer votre adresse email pour activer votre compte.\n"
					+ "Cliquez sur le lien suivant :\n\n"
					+ verificationLink + "\n\n"
					+ "Ce lien expire prochainement et ne peut être utilisé qu'une seule fois.\n"
					+ "Si vous n'êtes pas à l'origine de cette inscription, ignorez cet email.\n\n"
					+ "L'équipe Plumora"
			);
			mailSender.send(message);
		} catch (MessagingException | MailException exception) {
			// Same contract as SmtpPasswordResetMailer: an email delivery failure never surfaces to
			// the client as a 500, and never blocks the already-issued, already-usable token - the
			// caller keeps behaving as if the email was sent. POST /auth/resend-verification lets the
			// user (or the user retrying once SMTP is reachable again) request a fresh link.
			log.error("Failed to send the account verification email to {}", user.getEmail(), exception);
		}
	}
}
