package com.plumora.api.book.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A local mirror of one entry from Project Gutenberg's own official catalog CSV, downloaded
 * directly from gutenberg.org (see GutenbergCatalogSyncService). Never populated from
 * gutendex.com, which is blocked by Cloudflare from the production VPS.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "gutenberg_catalog_entries")
public class GutenbergCatalogEntry {

	@Id
	@Column(name = "gutenberg_id")
	private Integer gutenbergId;

	@Column(name = "title", nullable = false, length = 500)
	private String title;

	@Column(name = "authors", length = 500)
	private String authors;

	@Column(name = "language_code", nullable = false, length = 10)
	private String languageCode;

	@Column(name = "subjects")
	private String subjects;

	@Column(name = "bookshelves")
	private String bookshelves;

	@Column(name = "issued_date")
	private LocalDate issuedDate;

	@Column(name = "cover_url", length = 500)
	private String coverUrl;

	@Column(name = "content_url", nullable = false, length = 500)
	private String contentUrl;

	@Column(name = "synced_at", nullable = false)
	private LocalDateTime syncedAt;
}
