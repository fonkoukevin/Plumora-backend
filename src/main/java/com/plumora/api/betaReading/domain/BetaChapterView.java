package com.plumora.api.betaReading.domain;

import com.plumora.api.book.domain.Chapter;
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
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(
	name = "beta_chapter_views",
	uniqueConstraints = @UniqueConstraint(name = "uk_beta_chapter_views_chapter_reader", columnNames = {"chapter_id", "beta_reader_id"})
)
public class BetaChapterView {

	@Id
	@GeneratedValue
	@Column(name = "id_beta_chapter_view")
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

	@Column(name = "viewed_at", nullable = false)
	private LocalDateTime viewedAt;

	@PrePersist
	void onCreate() {
		if (viewedAt == null) {
			viewedAt = LocalDateTime.now();
		}
	}
}
