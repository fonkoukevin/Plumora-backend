package com.plumora.api.user.infrastructure;

import com.plumora.api.user.domain.PasswordResetToken;
import com.plumora.api.user.domain.User;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, UUID> {

	Optional<PasswordResetToken> findByToken(String token);

	List<PasswordResetToken> findByUserAndUsedAtIsNull(User user);
}
