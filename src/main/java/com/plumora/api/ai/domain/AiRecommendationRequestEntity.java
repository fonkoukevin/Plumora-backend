package com.plumora.api.ai.domain;

import com.plumora.api.user.domain.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "ai_recommendation_requests")
public class AiRecommendationRequestEntity {

	@Id
	@GeneratedValue
	@Column(name = "id_ai_recommendation_request")
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@Column(name = "query_text", nullable = false)
	private String queryText;

	@Column(name = "mood", length = 40)
	private String mood;

	@Column(name = "preferred_duration", length = 30)
	private String preferredDuration;

	@Column(name = "preferred_genre", length = 80)
	private String preferredGenre;

	@Column(name = "created_at", nullable = false)
	private LocalDateTime createdAt;

	@PrePersist
	void onCreate() {
		createdAt = LocalDateTime.now();
	}
}
