package com.plumora.api.user.infrastructure;

import com.plumora.api.user.domain.RoleName;
import com.plumora.api.user.domain.User;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, UUID> {
	List<User> findAllByOrderByCreatedAtDesc();

	List<User> findAllByRoles_Name(RoleName roleName);

	Optional<User> findByEmail(String email);

	Optional<User> findByUsername(String username);

	boolean existsByEmail(String email);

	boolean existsByUsername(String username);

	long countByActiveTrue();

	long countByRoles_Name(RoleName roleName);
}
