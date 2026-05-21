package com.plumora.api.betaReading.domain;

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
import jakarta.persistence.PreUpdate;
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
@Table(name = "beta_comments")
public class BetaComment {

	@Id
	@GeneratedValue
	@Column(name = "id_beta_comment")
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "campaign_id", nullable = false)
	private BetaReadingCampaign campaign;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "chapter_id", nullable = false)
	private Chapter chapter;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "beta_reader_id", nullable = false)
	private User betaReader;

	@Column(name = "comment_text", nullable = false)
	private String commentText;

	@Column(name = "selected_text")
	private String selectedText;

	@Column(name = "position_start")
	private Integer positionStart;

	@Column(name = "position_end")
	private Integer positionEnd;

	@Enumerated(EnumType.STRING)
	@Column(name = "feedback_type", nullable = false, length = 40)
	private BetaCommentFeedbackType feedbackType;

	@Enumerated(EnumType.STRING)
	@Column(name = "priority", nullable = false, length = 30)
	private BetaCommentPriority priority;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 30)
	private BetaCommentStatus status = BetaCommentStatus.OPEN;

	@Column(name = "created_at", nullable = false)
	private LocalDateTime createdAt;

	@Column(name = "updated_at")
	private LocalDateTime updatedAt;

	@PrePersist
	void onCreate() {
		createdAt = LocalDateTime.now();
		if (status == null) {
			status = BetaCommentStatus.OPEN;
		}
	}

	@PreUpdate
	void onUpdate() {
		updatedAt = LocalDateTime.now();
	}
}
