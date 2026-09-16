package com.plumora.api.user.infrastructure.mail;

import com.plumora.api.user.domain.User;

public interface AccountVerificationMailer {
	void sendVerificationLink(User user, String verificationLink);
}
