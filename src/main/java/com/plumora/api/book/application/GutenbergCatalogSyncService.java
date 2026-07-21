package com.plumora.api.book.application;

import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import com.plumora.api.book.infrastructure.GutenbergCatalogEntryRepository;
import com.plumora.api.book.infrastructure.gutenberg.GutenbergCatalogClient;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * Keeps a local mirror of Project Gutenberg's own official catalog (see
 * GutenbergCatalogClient), so book discovery/search and reading work entirely off our own
 * database and gutenberg.org content URLs, with no live call to gutendex.com (blocked by
 * Cloudflare from the production VPS).
 *
 * <p>The upsert writes go through JdbcTemplate batches rather than the JPA repository/entity
 * lifecycle: tens of thousands of rows per sync would otherwise bloat the persistence context
 * for a background job whose only real requirement is "get the rows into the table".
 */
@Service
public class GutenbergCatalogSyncService {

	private static final Logger log = LoggerFactory.getLogger(GutenbergCatalogSyncService.class);
	private static final int BATCH_SIZE = 1000;
	private static final String UPSERT_SQL = """
		insert into gutenberg_catalog_entries
			(gutenberg_id, title, authors, language_code, subjects, bookshelves, issued_date, cover_url, content_url, synced_at)
		values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
		on conflict (gutenberg_id) do update set
			title = excluded.title,
			authors = excluded.authors,
			language_code = excluded.language_code,
			subjects = excluded.subjects,
			bookshelves = excluded.bookshelves,
			issued_date = excluded.issued_date,
			cover_url = excluded.cover_url,
			content_url = excluded.content_url,
			synced_at = excluded.synced_at
		""";

	private final GutenbergCatalogClient gutenbergCatalogClient;
	private final GutenbergCatalogEntryRepository gutenbergCatalogEntryRepository;
	private final JdbcTemplate jdbcTemplate;
	private final CsvMapper csvMapper = new CsvMapper();

	public GutenbergCatalogSyncService(
		GutenbergCatalogClient gutenbergCatalogClient,
		GutenbergCatalogEntryRepository gutenbergCatalogEntryRepository,
		JdbcTemplate jdbcTemplate
	) {
		this.gutenbergCatalogClient = gutenbergCatalogClient;
		this.gutenbergCatalogEntryRepository = gutenbergCatalogEntryRepository;
		this.jdbcTemplate = jdbcTemplate;
	}

	@EventListener(ApplicationReadyEvent.class)
	public void syncOnStartupIfEmpty() {
		if (gutenbergCatalogEntryRepository.count() == 0) {
			log.info("Gutenberg catalog is empty - triggering an initial sync in the background.");
			syncCatalog();
		}
	}

	@Async
	@Scheduled(cron = "0 0 4 * * *")
	public void syncCatalog() {
		try {
			String csv = gutenbergCatalogClient.downloadCatalogCsv();
			int imported = parseAndUpsert(csv);
			log.info("Gutenberg catalog sync complete: {} entries upserted.", imported);
		} catch (Exception exception) {
			log.warn("Gutenberg catalog sync failed, keeping the existing data as-is.", exception);
		}
	}

	private int parseAndUpsert(String csv) throws java.io.IOException {
		CsvSchema schema = CsvSchema.emptySchema().withHeader();
		com.fasterxml.jackson.databind.MappingIterator<Map<String, String>> rows = csvMapper
			.readerFor(Map.class)
			.with(schema)
			.readValues(csv);

		List<Object[]> batch = new ArrayList<>(BATCH_SIZE);
		int total = 0;
		Timestamp now = Timestamp.valueOf(LocalDateTime.now());
		while (rows.hasNext()) {
			Object[] params = toRow(rows.next(), now);
			if (params != null) {
				batch.add(params);
			}
			if (batch.size() >= BATCH_SIZE) {
				jdbcTemplate.batchUpdate(UPSERT_SQL, batch);
				total += batch.size();
				batch.clear();
			}
		}
		if (!batch.isEmpty()) {
			jdbcTemplate.batchUpdate(UPSERT_SQL, batch);
			total += batch.size();
		}
		return total;
	}

	private Object[] toRow(Map<String, String> row, Timestamp now) {
		if (!"Text".equals(row.get("Type")) || !"en".equals(row.get("Language"))) {
			return null;
		}
		Integer gutenbergId = parseId(row.get("Text#"));
		String title = normalizeTitle(row.get("Title"));
		if (gutenbergId == null || !StringUtils.hasText(title)) {
			return null;
		}
		return new Object[] {
			gutenbergId,
			limit(title, 500),
			limit(row.get("Authors"), 500),
			"en",
			row.get("Subjects"),
			row.get("Bookshelves"),
			parseIssuedDate(row.get("Issued")),
			"https://www.gutenberg.org/cache/epub/" + gutenbergId + "/pg" + gutenbergId + ".cover.medium.jpg",
			"https://www.gutenberg.org/files/" + gutenbergId + "/" + gutenbergId + "-0.txt",
			now
		};
	}

	private Integer parseId(String value) {
		if (!StringUtils.hasText(value)) {
			return null;
		}
		try {
			return Integer.valueOf(value.trim());
		} catch (NumberFormatException exception) {
			return null;
		}
	}

	private String normalizeTitle(String title) {
		if (!StringUtils.hasText(title)) {
			return null;
		}
		return title.replace('\n', ' ').replace('\r', ' ').trim();
	}

	private Date parseIssuedDate(String value) {
		if (!StringUtils.hasText(value)) {
			return null;
		}
		try {
			return Date.valueOf(LocalDate.parse(value.trim()));
		} catch (DateTimeParseException exception) {
			return null;
		}
	}

	private String limit(String value, int maxLength) {
		if (value == null) {
			return null;
		}
		String trimmed = value.trim();
		return trimmed.length() <= maxLength ? trimmed : trimmed.substring(0, maxLength);
	}
}
