package com.plumora.api.book.infrastructure.gutendex;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class GutendexClientTest {

	@Test
	void searchBooksMapsGutendexResponse() {
		RestClient.Builder builder = RestClient.builder().baseUrl("https://gutendex.test");
		MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
		GutendexClient client = new GutendexClient(builder.build());
		String response = """
			{
			  "count": 1,
			  "next": null,
			  "previous": null,
			  "results": [
			    {
			      "id": 123,
			      "title": "Les Miserables",
			      "authors": [{"name": "Victor Hugo", "birth_year": 1802, "death_year": 1885}],
			      "summaries": ["Un roman social."],
			      "subjects": ["French fiction"],
			      "bookshelves": ["FR Litterature"],
			      "languages": ["fr"],
			      "copyright": false,
			      "media_type": "Text",
			      "formats": {"text/html": "https://example.test/read.html"},
			      "download_count": 42
			    }
			  ]
			}
			""";

		server.expect(requestTo("https://gutendex.test/books?page=2&sort=popular&copyright=false&search=Hugo&languages=fr&topic=fiction"))
			.andRespond(withSuccess(response, MediaType.APPLICATION_JSON));

		GutendexPageResponse page = client.searchBooks(
			GutendexSearchRequest.publicDomain("Hugo", "fr", "fiction", 2)
		);

		assertThat(page.count()).isEqualTo(1);
		assertThat(page.results()).hasSize(1);
		assertThat(page.results().getFirst().id()).isEqualTo(123);
		assertThat(page.results().getFirst().authors().getFirst().name()).isEqualTo("Victor Hugo");
		assertThat(page.results().getFirst().downloadCount()).isEqualTo(42);
		server.verify();
	}

	@Test
	void getBookReturnsEmptyForGutendex404() {
		RestClient.Builder builder = RestClient.builder().baseUrl("https://gutendex.test");
		MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
		GutendexClient client = new GutendexClient(builder.build());

		server.expect(requestTo("https://gutendex.test/books/404"))
			.andRespond(withStatus(HttpStatus.NOT_FOUND));

		assertThat(client.getBook(404)).isEmpty();
		server.verify();
	}
}
