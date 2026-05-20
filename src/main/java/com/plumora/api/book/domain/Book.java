package com.plumora.api.book.domain;

import com.plumora.api.user.domain.User;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "books")
public class Book {

	@Id
	@GeneratedValue
	@Column(name = "id_book")
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "author_id", nullable = false)
	private User author;

	@Column(name = "title", nullable = false, length = 150)
	private String title;

	@Column(name = "subtitle", length = 200)
	private String subtitle;

	@Column(name = "summary")
	private String summary;

	@Column(name = "cover_url", length = 500)
	private String coverUrl;

	@Column(name = "genre", nullable = false, length = 80)
	private String genre;

	@Column(name = "language_code", length = 10)
	private String languageCode = "fr";

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 40)
	private BookStatus status = BookStatus.DRAFT;

	@Enumerated(EnumType.STRING)
	@Column(name = "visibility", nullable = false, length = 40)
	private BookVisibility visibility = BookVisibility.PRIVATE;

	@Column(name = "published_at")
	private LocalDateTime publishedAt;

	@Column(name = "reading_count")
	private int readingCount;

	@Column(name = "average_rating")
	private BigDecimal averageRating = BigDecimal.ZERO;

	@Column(name = "created_at", nullable = false)
	private LocalDateTime createdAt;

	@Column(name = "updated_at")
	private LocalDateTime updatedAt;

	@OneToMany(mappedBy = "book", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<Chapter> chapters = new ArrayList<>();

	@PrePersist
	void onCreate() {
		createdAt = LocalDateTime.now();
	}

	@PreUpdate
	void onUpdate() {
		updatedAt = LocalDateTime.now();
	}
}
