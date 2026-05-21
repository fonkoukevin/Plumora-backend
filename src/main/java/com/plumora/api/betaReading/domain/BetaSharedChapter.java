package com.plumora.api.betaReading.domain;

import com.plumora.api.book.domain.Chapter;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(
	name = "beta_shared_chapters",
	uniqueConstraints = @UniqueConstraint(name = "uk_beta_shared_chapters_campaign_chapter", columnNames = {"campaign_id", "chapter_id"})
)
public class BetaSharedChapter {

	@Id
	@GeneratedValue
	@Column(name = "id_beta_shared_chapter")
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "campaign_id", nullable = false)
	private BetaReadingCampaign campaign;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "chapter_id", nullable = false)
	private Chapter chapter;
}
