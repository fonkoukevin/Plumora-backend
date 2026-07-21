package com.plumora.api.book.infrastructure.openlibrary;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.plumora.api.shared.exception.ExternalServiceUnavailableException;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

class OpenLibraryClientTest {

	@Test
	void searchBooksCombinesSearchAndSubjectIntoTheQueryParam() {
		RestClient.Builder builder = RestClient.builder().baseUrl("https://openlibrary.test");
		MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
		OpenLibraryClient client = new OpenLibraryClient(builder.build());
		String response = """
			{
			  "numFound": 1,
			  "docs": [
			    {
			      "key": "/works/OL1W",
			      "title": "Les Miserables",
			      "author_name": ["Victor Hugo"],
			      "cover_i": 987,
			      "subject": ["Fantasy"]
			    }
			  ]
			}
			""";

		server.expect(request -> assertQuery(request.getURI(), "Hugo subject:fiction"))
			.andRespond(withSuccess(response, MediaType.APPLICATION_JSON));

		OpenLibrarySearchResponse result = client.searchBooks("Hugo", "fiction", 2);

		assertThat(result.numFound()).isEqualTo(1);
		assertThat(result.docs()).hasSize(1);
		assertThat(result.docs().getFirst().title()).isEqualTo("Les Miserables");
		assertThat(result.docs().getFirst().coverId()).isEqualTo(987);
		server.verify();
	}

	@Test
	void searchBooksUsesWildcardQueryWhenNoFiltersAreProvided() {
		RestClient.Builder builder = RestClient.builder().baseUrl("https://openlibrary.test");
		MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
		OpenLibraryClient client = new OpenLibraryClient(builder.build());

		server.expect(request -> assertQuery(request.getURI(), "*"))
			.andRespond(withSuccess("{\"numFound\": 0, \"docs\": []}", MediaType.APPLICATION_JSON));

		OpenLibrarySearchResponse result = client.searchBooks(null, "  ", 0);

		assertThat(result.numFound()).isZero();
		assertThat(result.docs()).isEmpty();
		server.verify();
	}

	@Test
	void searchBooksThrowsWhenOpenLibraryReturnsAnErrorStatus() {
		RestClient.Builder builder = RestClient.builder().baseUrl("https://openlibrary.test");
		MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
		OpenLibraryClient client = new OpenLibraryClient(builder.build());

		server.expect(request -> assertQuery(request.getURI(), "*"))
			.andRespond(withStatus(HttpStatus.FORBIDDEN));

		assertThatThrownBy(() -> client.searchBooks(null, null, 1))
			.isInstanceOf(ExternalServiceUnavailableException.class)
			.hasMessage("Open Library is currently unavailable");
		server.verify();
	}

	private void assertQuery(URI uri, String expectedQuery) {
		var params = UriComponentsBuilder.fromUri(uri).build().getQueryParams();
		assertThat(decode(params.getFirst("q"))).isEqualTo(expectedQuery);
		assertThat(decode(params.getFirst("fields"))).isEqualTo("key,title,author_name,cover_i,isbn,subject");
	}

	private String decode(String value) {
		return URLDecoder.decode(value, StandardCharsets.UTF_8);
	}
}
