package com.plumora.api.book.infrastructure.openlibrary;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.net.URLDecoder;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

class OpenLibraryCoverServiceTest {

	@Test
	void resolveCoverUrlUsesCoverIdAndCachesResult() {
		RestClient.Builder builder = RestClient.builder().baseUrl("https://openlibrary.test");
		MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
		OpenLibraryCoverService service = new OpenLibraryCoverService(
			builder.build(),
			Clock.fixed(Instant.parse("2026-07-03T10:00:00Z"), ZoneOffset.UTC)
		);
		String response = """
			{
			  "docs": [
			    {
			      "key": "/works/OL1W",
			      "title": "Les Miserables",
			      "author_name": ["Victor Hugo"],
			      "cover_i": 987
			    }
			  ]
			}
			""";

		server.expect(request -> assertSearchRequest(request.getURI(), "Les Miserables", "Victor Hugo"))
			.andRespond(withSuccess(response, MediaType.APPLICATION_JSON));

		String first = service.resolveCoverUrl("Les Miserables", "Victor Hugo");
		String second = service.resolveCoverUrl("Les Miserables", "Victor Hugo");

		assertThat(first).isEqualTo("https://covers.openlibrary.org/b/id/987-L.jpg?default=false");
		assertThat(second).isEqualTo(first);
		server.verify();
	}

	@Test
	void resolveCoverUrlFallsBackToIsbnWhenCoverIdIsMissing() {
		RestClient.Builder builder = RestClient.builder().baseUrl("https://openlibrary.test");
		MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
		OpenLibraryCoverService service = new OpenLibraryCoverService(builder.build(), Clock.systemUTC());
		String response = """
			{
			  "docs": [
			    {
			      "key": "/works/OL2W",
			      "title": "Book",
			      "author_name": ["Author"],
			      "isbn": ["9781234567890"]
			    }
			  ]
			}
			""";

		server.expect(request -> assertSearchRequest(request.getURI(), "Book", "Author"))
			.andRespond(withSuccess(response, MediaType.APPLICATION_JSON));

		assertThat(service.resolveCoverUrl("Book", "Author"))
			.isEqualTo("https://covers.openlibrary.org/b/isbn/9781234567890-L.jpg?default=false");
		server.verify();
	}

	@Test
	void resolveCoverUrlReturnsNullWhenOpenLibraryIsUnavailable() {
		RestClient.Builder builder = RestClient.builder().baseUrl("https://openlibrary.test");
		MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
		OpenLibraryCoverService service = new OpenLibraryCoverService(builder.build(), Clock.systemUTC());

		server.expect(request -> assertSearchRequest(request.getURI(), "Book", "Author"))
			.andRespond(withStatus(HttpStatus.BAD_GATEWAY));

		assertThat(service.resolveCoverUrl("Book", "Author")).isNull();
		server.verify();
	}

	private void assertSearchRequest(URI uri, String title, String author) {
		assertThat(uri.getScheme() + "://" + uri.getHost() + uri.getPath())
			.isEqualTo("https://openlibrary.test/search.json");
		var params = UriComponentsBuilder.fromUri(uri).build().getQueryParams();
		assertThat(decode(params.getFirst("title"))).isEqualTo(title);
		assertThat(decode(params.getFirst("author"))).isEqualTo(author);
		assertThat(decode(params.getFirst("fields"))).isEqualTo("key,title,author_name,cover_i,isbn");
		assertThat(decode(params.getFirst("limit"))).isEqualTo("1");
	}

	private String decode(String value) {
		return URLDecoder.decode(value, StandardCharsets.UTF_8);
	}
}
