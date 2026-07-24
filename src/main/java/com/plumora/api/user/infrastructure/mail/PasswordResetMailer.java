package com.plumora.api.user.infrastructure.mail;

import com.plumora.api.user.domain.User;

public interface PasswordResetMailer {
	void sendResetLink(User user, String resetLink);
}
