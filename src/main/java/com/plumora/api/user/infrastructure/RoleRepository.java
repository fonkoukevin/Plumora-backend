package com.plumora.api.user.infrastructure;

import com.plumora.api.user.domain.Role;
import com.plumora.api.user.domain.RoleName;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoleRepository extends JpaRepository<Role, UUID> {
	Optional<Role> findByName(RoleName name);
}
