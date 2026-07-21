package com.plumora.api.book.infrastructure.gutenberg;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.plumora.api.shared.exception.ExternalServiceUnavailableException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class GutenbergCatalogClientTest {

	@Test
	void downloadCatalogCsvReturnsTheResponseBody() {
		RestClient.Builder builder = RestClient.builder().baseUrl("https://gutenberg.test/pg_catalog.csv");
		MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
		GutenbergCatalogClient client = new GutenbergCatalogClient(builder.build());
		String csv = "Text#,Type,Issued,Title,Language,Authors,Subjects,LoCC,Bookshelves\n1,Text,1971-12-01,A Book,en,An Author,A Subject,E1,A Shelf\n";

		server.expect(request -> assertThat(request.getURI().toString()).isEqualTo("https://gutenberg.test/pg_catalog.csv"))
			.andRespond(withSuccess(csv, MediaType.parseMediaType("text/csv")));

		assertThat(client.downloadCatalogCsv()).isEqualTo(csv);
		server.verify();
	}

	@Test
	void downloadCatalogCsvThrowsWhenGutenbergReturnsAnErrorStatus() {
		RestClient.Builder builder = RestClient.builder().baseUrl("https://gutenberg.test/pg_catalog.csv");
		MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
		GutenbergCatalogClient client = new GutenbergCatalogClient(builder.build());

		server.expect(request -> {})
			.andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));

		assertThatThrownBy(client::downloadCatalogCsv)
			.isInstanceOf(ExternalServiceUnavailableException.class)
			.hasMessage("Gutenberg catalog is currently unavailable");
		server.verify();
	}
}
