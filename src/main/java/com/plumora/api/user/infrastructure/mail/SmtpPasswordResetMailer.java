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

@Component
@ConditionalOnProperty(name = "plumora.mail.provider", havingValue = "smtp")
public class SmtpPasswordResetMailer implements PasswordResetMailer {

	private static final Logger log = LoggerFactory.getLogger(SmtpPasswordResetMailer.class);

	private final JavaMailSender mailSender;
	private final String fromAddress;

	public SmtpPasswordResetMailer(
		JavaMailSender mailSender,
		@Value("${plumora.mail.from-address}") String fromAddress
	) {
		this.mailSender = mailSender;
		this.fromAddress = fromAddress;
	}

	@Override
	public void sendResetLink(User user, String resetLink) {
		try {
			MimeMessage message = mailSender.createMimeMessage();
			MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");
			helper.setFrom(fromAddress);
			helper.setTo(user.getEmail());
			helper.setSubject("Réinitialisation de votre mot de passe Plumora");
			helper.setText(
				"Bonjour " + user.getFirstname() + ",\n\n"
					+ "Vous avez demandé la réinitialisation de votre mot de passe Plumora.\n"
					+ "Cliquez sur le lien suivant pour choisir un nouveau mot de passe :\n\n"
					+ resetLink + "\n\n"
					+ "Ce lien expire prochainement et ne peut être utilisé qu'une seule fois.\n"
					+ "Si vous n'êtes pas à l'origine de cette demande, ignorez cet email.\n\n"
					+ "L'équipe Plumora"
			);
			mailSender.send(message);
		} catch (MessagingException | MailException exception) {
			// Never let an email delivery failure surface to the client as a 500 with the SMTP
			// provider's own error details, and never let it block the (already-issued,
			// already-usable) token: the caller keeps behaving as if the email was sent, matching
			// the anti-enumeration contract (POST /auth/forgot-password always responds 200).
			log.error("Failed to send the password reset email to {}", user.getEmail(), exception);
		}
	}
}
