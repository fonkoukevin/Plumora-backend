package com.plumora.api.user.infrastructure;

import com.plumora.api.user.domain.EmailVerificationToken;
import com.plumora.api.user.domain.User;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmailVerificationTokenRepository extends JpaRepository<EmailVerificationToken, UUID> {

	Optional<EmailVerificationToken> findByToken(String token);

	List<EmailVerificationToken> findByUserAndUsedAtIsNull(User user);
}
