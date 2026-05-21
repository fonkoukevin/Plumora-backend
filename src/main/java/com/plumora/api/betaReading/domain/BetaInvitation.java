package com.plumora.api.betaReading.domain;

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
	name = "beta_invitations",
	uniqueConstraints = @UniqueConstraint(name = "uk_beta_invitations_campaign_reader", columnNames = {"campaign_id", "beta_reader_id"})
)
public class BetaInvitation {

	@Id
	@GeneratedValue
	@Column(name = "id_beta_invitation")
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "campaign_id", nullable = false)
	private BetaReadingCampaign campaign;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "beta_reader_id", nullable = false)
	private User betaReader;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 30)
	private BetaInvitationStatus status = BetaInvitationStatus.PENDING;

	@Column(name = "invited_at", nullable = false)
	private LocalDateTime invitedAt;

	@Column(name = "responded_at")
	private LocalDateTime respondedAt;

	@PrePersist
	void onCreate() {
		invitedAt = LocalDateTime.now();
		if (status == null) {
			status = BetaInvitationStatus.PENDING;
		}
	}
}
