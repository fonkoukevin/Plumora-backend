package com.plumora.api.reading.domain;

import com.plumora.api.book.domain.Book;
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
import java.math.BigDecimal;
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
	name = "reading_progress",
	uniqueConstraints = @UniqueConstraint(name = "uk_reading_progress_user_book", columnNames = {"user_id", "book_id"})
)
public class ReadingProgress {

	@Id
	@GeneratedValue
	@Column(name = "id_reading_progress")
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "book_id", nullable = false)
	private Book book;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "current_chapter_id")
	private Chapter currentChapter;

	@Column(name = "progress_percentage", precision = 5, scale = 2)
	private BigDecimal progressPercentage = BigDecimal.ZERO;

	@Column(name = "started_at", nullable = false)
	private LocalDateTime startedAt;

	@Column(name = "last_read_at")
	private LocalDateTime lastReadAt;

	@Column(name = "finished_at")
	private LocalDateTime finishedAt;

	@PrePersist
	void onCreate() {
		LocalDateTime now = LocalDateTime.now();
		if (startedAt == null) {
			startedAt = now;
		}
		if (lastReadAt == null) {
			lastReadAt = startedAt;
		}
		if (progressPercentage == null) {
			progressPercentage = BigDecimal.ZERO;
		}
	}
}
