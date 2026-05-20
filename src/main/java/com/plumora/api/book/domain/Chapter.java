package com.plumora.api.book.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "chapters")
public class Chapter {

	@Id
	@GeneratedValue
	@Column(name = "id_chapter")
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "book_id", nullable = false)
	private Book book;

	@Column(name = "title", nullable = false, length = 150)
	private String title;

	@Column(name = "content")
	private String content;

	@Column(name = "chapter_order", nullable = false)
	private int chapterOrder;

	@Column(name = "word_count")
	private int wordCount;

	@Column(name = "created_at", nullable = false)
	private LocalDateTime createdAt;

	@Column(name = "updated_at")
	private LocalDateTime updatedAt;

	@PrePersist
	void onCreate() {
		createdAt = LocalDateTime.now();
		updateWordCount();
	}

	@PreUpdate
	void onUpdate() {
		updatedAt = LocalDateTime.now();
		updateWordCount();
	}

	public void updateWordCount() {
		if (content == null || content.isBlank()) {
			wordCount = 0;
			return;
		}
		wordCount = content.trim().split("\\s+").length;
	}
}
