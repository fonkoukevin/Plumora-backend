package com.plumora.api.admin.domain;

import com.plumora.api.user.domain.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "admin_audit_logs")
public class AdminAuditLog {

	@Id
	@GeneratedValue
	@Column(name = "id_admin_audit_log")
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "admin_id", nullable = false)
	private User admin;

	@Column(name = "admin_email", nullable = false, length = 150)
	private String adminEmail;

	@Enumerated(EnumType.STRING)
	@Column(name = "action", nullable = false, length = 50)
	private AdminAction action;

	@Enumerated(EnumType.STRING)
	@Column(name = "target_type", nullable = false, length = 50)
	private AdminTargetType targetType;

	@Column(name = "target_id")
	private UUID targetId;

	@Column(name = "description", length = 2000)
	private String description;

	@Column(name = "created_at", nullable = false)
	private LocalDateTime createdAt;

	@PrePersist
	void onCreate() {
		createdAt = LocalDateTime.now();
	}
}
