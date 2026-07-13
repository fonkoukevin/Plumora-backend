package com.plumora.api.user.infrastructure;

import com.plumora.api.user.domain.RoleName;
import com.plumora.api.user.domain.User;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, UUID> {
	List<User> findAllByOrderByCreatedAtDesc();

	List<User> findAllByRoles_Name(RoleName roleName);

	Optional<User> findByEmail(String email);

	Optional<User> findByUsername(String username);

	boolean existsByEmail(String email);

	boolean existsByUsername(String username);

	long countByActiveTrue();

	long countByRoles_Name(RoleName roleName);

	@Query("""
		select distinct u from User u
		left join u.roles r
		where (:query is null
				or lower(u.username) like :query
				or lower(u.email) like :query
				or lower(coalesce(u.firstname, '')) like :query
				or lower(coalesce(u.lastname, '')) like :query
			)
			and (:role is null or r.name = :role)
			and (:active is null or u.active = :active)
		order by u.createdAt desc
		""")
	List<User> search(
		@Param("query") String query,
		@Param("role") RoleName role,
		@Param("active") Boolean active
	);
}
