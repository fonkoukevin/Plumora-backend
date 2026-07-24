package com.plumora.api.user.infrastructure.mail;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.plumora.api.user.domain.User;
import jakarta.mail.internet.MimeMessage;
import java.util.Properties;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;

@ExtendWith(MockitoExtension.class)
class SmtpPasswordResetMailerTest {

	@Mock
	private JavaMailSender mailSender;

	private SmtpPasswordResetMailer mailer;

	@BeforeEach
	void setUp() {
		mailer = new SmtpPasswordResetMailer(mailSender, "no-reply@plumora-books.fr", "smtp-account@gmail.com");
	}

	@Test
	void sendsAnEmailToTheUserWithTheResetLink() throws Exception {
		MimeMessage mimeMessage = new MimeMessage(jakarta.mail.Session.getInstance(new Properties()));
		when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

		User user = user("reader@example.com");
		mailer.sendResetLink(user, "https://app.plumora-books.fr/reset-password?token=abc123");

		verify(mailSender).send(mimeMessage);
		assertThat(mimeMessage.getAllRecipients()[0].toString()).isEqualTo("reader@example.com");
		assertThat(mimeMessage.getFrom()[0].toString()).isEqualTo("no-reply@plumora-books.fr");
		assertThat(mimeMessage.getSubject()).contains("mot de passe");
		assertThat((String) mimeMessage.getContent()).contains("https://app.plumora-books.fr/reset-password?token=abc123");
	}

	@Test
	void fallsBackToTheSmtpUsernameWhenFromAddressIsSetButBlank() throws Exception {
		// Regression test: a real deploy had MAIL_FROM= (present but empty) in .env, which Spring's
		// ${MAIL_FROM:${SMTP_USERNAME:}} placeholder does NOT treat as "absent" - every send failed
		// with jakarta.mail.internet.AddressException: Illegal address until the fallback was moved
		// to Java (StringUtils.hasText), which this test locks in.
		SmtpPasswordResetMailer mailerWithBlankFrom = new SmtpPasswordResetMailer(mailSender, "", "smtp-account@gmail.com");
		MimeMessage mimeMessage = new MimeMessage(jakarta.mail.Session.getInstance(new Properties()));
		when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

		mailerWithBlankFrom.sendResetLink(user("reader@example.com"), "https://app.plumora-books.fr/reset-password?token=abc123");

		assertThat(mimeMessage.getFrom()[0].toString()).isEqualTo("smtp-account@gmail.com");
	}

	@Test
	void doesNotThrowWhenTheProviderFailsToSend() {
		MimeMessage mimeMessage = new MimeMessage(jakarta.mail.Session.getInstance(new Properties()));
		when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
		doThrow(new MailSendException("SMTP connection refused")).when(mailSender).send(any(MimeMessage.class));

		User user = user("reader@example.com");

		mailer.sendResetLink(user, "https://app.plumora-books.fr/reset-password?token=abc123");
	}

	private User user(String email) {
		User user = new User();
		user.setId(UUID.randomUUID());
		user.setFirstname("Reader");
		user.setEmail(email);
		return user;
	}
}
