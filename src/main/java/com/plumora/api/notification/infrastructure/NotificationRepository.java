package com.plumora.api.notification.infrastructure;

import com.plumora.api.notification.domain.Notification;
import com.plumora.api.user.domain.User;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {
	@EntityGraph(attributePaths = "user")
	List<Notification> findByUserOrderByCreatedAtDesc(User user);

	long countByUserAndReadFalse(User user);

	List<Notification> findByUserAndReadFalse(User user);

	@EntityGraph(attributePaths = "user")
	@Query("select n from Notification n where n.id = :id")
	Optional<Notification> findByIdWithUser(@Param("id") UUID id);
}
