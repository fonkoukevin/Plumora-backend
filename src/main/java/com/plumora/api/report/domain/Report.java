package com.plumora.api.report.domain;

import com.plumora.api.book.domain.Book;
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
@Table(name = "reports")
public class Report {

	@Id
	@GeneratedValue
	@Column(name = "id_report")
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "reporter_id", nullable = false)
	private User reporter;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "book_id", nullable = false)
	private Book book;

	@Column(name = "reason", nullable = false, length = 100)
	private String reason;

	@Column(name = "description")
	private String description;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 30)
	private ReportStatus status = ReportStatus.OPEN;

	@Column(name = "created_at", nullable = false)
	private LocalDateTime createdAt;

	@Column(name = "resolved_at")
	private LocalDateTime resolvedAt;

	@PrePersist
	void onCreate() {
		createdAt = LocalDateTime.now();
	}
}
