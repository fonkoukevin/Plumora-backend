package com.plumora.api.betaReading.domain;

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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "beta_reading_campaigns")
public class BetaReadingCampaign {

	@Id
	@GeneratedValue
	@Column(name = "id_beta_reading_campaign")
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "book_id", nullable = false)
	private Book book;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "author_id", nullable = false)
	private User author;

	@Column(name = "title", nullable = false, length = 150)
	private String title;

	@Column(name = "instructions")
	private String instructions;

	@Column(name = "deadline")
	private LocalDate deadline;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 30)
	private BetaCampaignStatus status = BetaCampaignStatus.ACTIVE;

	@Column(name = "created_at", nullable = false)
	private LocalDateTime createdAt;

	@Column(name = "closed_at")
	private LocalDateTime closedAt;

	@PrePersist
	void onCreate() {
		createdAt = LocalDateTime.now();
		if (status == null) {
			status = BetaCampaignStatus.ACTIVE;
		}
	}
}
