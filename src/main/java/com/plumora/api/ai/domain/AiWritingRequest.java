package com.plumora.api.ai.domain;

import com.plumora.api.book.domain.Chapter;
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
@Table(name = "ai_writing_requests")
public class AiWritingRequest {

	@Id
	@GeneratedValue
	@Column(name = "id_ai_writing_request")
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "chapter_id", nullable = false)
	private Chapter chapter;

	@Column(name = "selected_text", nullable = false)
	private String selectedText;

	@Column(name = "context_text")
	private String contextText;

	@Enumerated(EnumType.STRING)
	@Column(name = "action_type", nullable = false, length = 50)
	private AiWritingActionType actionType;

	@Column(name = "created_at", nullable = false)
	private LocalDateTime createdAt;

	@PrePersist
	void onCreate() {
		createdAt = LocalDateTime.now();
	}
}
