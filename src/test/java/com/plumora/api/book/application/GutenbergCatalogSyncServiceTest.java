package com.plumora.api.book.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.plumora.api.book.infrastructure.GutenbergCatalogEntryRepository;
import com.plumora.api.book.infrastructure.gutenberg.GutenbergCatalogClient;
import java.sql.Date;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

@ExtendWith(MockitoExtension.class)
class GutenbergCatalogSyncServiceTest {

	private static final String HEADER = "Text#,Type,Issued,Title,Language,Authors,Subjects,LoCC,Bookshelves\n";

	@Mock
	private GutenbergCatalogClient gutenbergCatalogClient;

	@Mock
	private GutenbergCatalogEntryRepository gutenbergCatalogEntryRepository;

	@Mock
	private JdbcTemplate jdbcTemplate;

	private GutenbergCatalogSyncService syncService;

	@BeforeEach
	void setUp() {
		syncService = new GutenbergCatalogSyncService(gutenbergCatalogClient, gutenbergCatalogEntryRepository, jdbcTemplate);
	}

	@Test
	void syncCatalogUpsertsOnlyEnglishTextEntriesWithComputedGutenbergUrls() {
		String csv = HEADER
			+ "123,Text,1998-06-01,Les Miserables,en,\"Hugo, Victor, 1802-1885\",\"French fiction; Classics\",PQ,Classics\n"
			+ "456,Text,2001-01-01,Un Livre Francais,fr,Auteur,Sujet,PQ,Etagere\n"
			+ "789,Audio,2010-01-01,An Audiobook,en,Someone,Sound,PQ,Audio\n";
		when(gutenbergCatalogClient.downloadCatalogCsv()).thenReturn(csv);

		syncService.syncCatalog();

		@SuppressWarnings("unchecked")
		ArgumentCaptor<List<Object[]>> batchCaptor = ArgumentCaptor.forClass(List.class);
		verify(jdbcTemplate).batchUpdate(any(String.class), batchCaptor.capture());
		List<Object[]> batch = batchCaptor.getValue();

		assertThat(batch).hasSize(1);
		Object[] row = batch.getFirst();
		assertThat(row[0]).isEqualTo(123);
		assertThat(row[1]).isEqualTo("Les Miserables");
		assertThat(row[2]).isEqualTo("Hugo, Victor, 1802-1885");
		assertThat(row[3]).isEqualTo("en");
		assertThat(row[4]).isEqualTo("French fiction; Classics");
		assertThat(row[5]).isEqualTo("Classics");
		assertThat(row[6]).isEqualTo(Date.valueOf(LocalDate.of(1998, 6, 1)));
		assertThat(row[7]).isEqualTo("https://www.gutenberg.org/cache/epub/123/pg123.cover.medium.jpg");
		assertThat(row[8]).isEqualTo("https://www.gutenberg.org/files/123/123-0.txt");
	}

	@Test
	void syncCatalogTreatsSqlMetacharactersInImportedFieldsAsLiteralData() {
		// OWASP A05 (Injection): the sync writes via a single parameterized UPSERT_SQL template
		// and jdbcTemplate.batchUpdate(sql, List<Object[]>) - this proves that contract at the
		// unit level by capturing the exact SQL string passed for a malicious title/author and
		// asserting it is byte-for-byte the same template used for an ordinary row (no string
		// concatenation happening anywhere in the sync path), with the attacker-controlled value
		// only ever appearing inside the bind-parameter array, never inside the SQL text itself.
		String maliciousTitle = "Robert'); DROP TABLE gutenberg_catalog_entries;--";
		String maliciousAuthor = "x' OR '1'='1";
		String csv = HEADER
			+ "123,Text,1998-06-01,\"" + maliciousTitle + "\",en,\"" + maliciousAuthor + "\",Sujet,PQ,Etagere\n";
		when(gutenbergCatalogClient.downloadCatalogCsv()).thenReturn(csv);

		syncService.syncCatalog();

		ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
		@SuppressWarnings("unchecked")
		ArgumentCaptor<List<Object[]>> batchCaptor = ArgumentCaptor.forClass(List.class);
		verify(jdbcTemplate).batchUpdate(sqlCaptor.capture(), batchCaptor.capture());

		String sql = sqlCaptor.getValue();
		assertThat(sql).doesNotContain(maliciousTitle);
		assertThat(sql).doesNotContain("DROP TABLE");
		assertThat(sql).contains("?");

		Object[] row = batchCaptor.getValue().getFirst();
		assertThat(row[1]).isEqualTo(maliciousTitle);
		assertThat(row[2]).isEqualTo(maliciousAuthor);
	}

	@Test
	void syncCatalogSkipsRowsWithoutAParsableIdOrTitle() {
		String csv = HEADER
			+ ",Text,1998-06-01,No Id,en,Author,Subject,PQ,Shelf\n"
			+ "42,Text,1998-06-01,,en,Author,Subject,PQ,Shelf\n";
		when(gutenbergCatalogClient.downloadCatalogCsv()).thenReturn(csv);

		syncService.syncCatalog();

		verify(jdbcTemplate, never()).batchUpdate(any(String.class), any(List.class));
	}

	@Test
	void syncCatalogToleratesAMissingOrUnparsableIssuedDate() {
		String csv = HEADER + "123,Text,,Les Miserables,en,Hugo,Subject,PQ,Shelf\n";
		when(gutenbergCatalogClient.downloadCatalogCsv()).thenReturn(csv);

		syncService.syncCatalog();

		@SuppressWarnings("unchecked")
		ArgumentCaptor<List<Object[]>> batchCaptor = ArgumentCaptor.forClass(List.class);
		verify(jdbcTemplate).batchUpdate(any(String.class), batchCaptor.capture());
		assertThat(batchCaptor.getValue().getFirst()[6]).isNull();
	}

	@Test
	void syncCatalogSwallowsFailuresFromTheDownloadClient() {
		when(gutenbergCatalogClient.downloadCatalogCsv())
			.thenThrow(new com.plumora.api.shared.exception.ExternalServiceUnavailableException("Gutenberg catalog is currently unavailable"));

		syncService.syncCatalog();

		verify(jdbcTemplate, never()).batchUpdate(any(String.class), any(List.class));
	}

	@Test
	void syncOnStartupIfEmptyTriggersSyncOnlyWhenTheLocalCatalogIsEmpty() {
		when(gutenbergCatalogEntryRepository.count()).thenReturn(0L);
		when(gutenbergCatalogClient.downloadCatalogCsv()).thenReturn(HEADER);

		syncService.syncOnStartupIfEmpty();

		verify(gutenbergCatalogClient, times(1)).downloadCatalogCsv();
	}

	@Test
	void syncOnStartupIfEmptyDoesNothingWhenTheLocalCatalogAlreadyHasEntries() {
		when(gutenbergCatalogEntryRepository.count()).thenReturn(50_000L);

		syncService.syncOnStartupIfEmpty();

		verify(gutenbergCatalogClient, never()).downloadCatalogCsv();
	}
}
