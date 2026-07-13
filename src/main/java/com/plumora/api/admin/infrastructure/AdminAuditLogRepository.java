package com.plumora.api.admin.infrastructure;

import com.plumora.api.admin.domain.AdminAction;
import com.plumora.api.admin.domain.AdminAuditLog;
import com.plumora.api.admin.domain.AdminTargetType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AdminAuditLogRepository extends JpaRepository<AdminAuditLog, UUID> {

	@EntityGraph(attributePaths = "admin")
	List<AdminAuditLog> findTop10ByOrderByCreatedAtDesc();

	@EntityGraph(attributePaths = "admin")
	@Query("""
		select l from AdminAuditLog l
		where (:action is null or l.action = :action)
			and (:adminId is null or l.admin.id = :adminId)
			and (:targetType is null or l.targetType = :targetType)
			and (:dateFrom is null or l.createdAt >= :dateFrom)
			and (:dateTo is null or l.createdAt <= :dateTo)
		order by l.createdAt desc
		""")
	List<AdminAuditLog> search(
		@Param("action") AdminAction action,
		@Param("adminId") UUID adminId,
		@Param("targetType") AdminTargetType targetType,
		@Param("dateFrom") LocalDateTime dateFrom,
		@Param("dateTo") LocalDateTime dateTo,
		Pageable pageable
	);
}
