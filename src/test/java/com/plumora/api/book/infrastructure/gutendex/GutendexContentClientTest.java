package com.plumora.api.book.infrastructure.gutendex;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.plumora.api.shared.exception.ExternalServiceUnavailableException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class GutendexContentClientTest {

	@Test
	void downloadFollowsRedirectsBeforeReturningContent() {
		RestClient.Builder builder = RestClient.builder();
		MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
		GutendexContentClient client = new GutendexContentClient(builder.build());

		server.expect(requestTo("http://www.gutenberg.org/cache/epub/11/pg11.txt"))
			.andRespond(withStatus(HttpStatus.FOUND)
				.header(HttpHeaders.LOCATION, "https://www.gutenberg.org/cache/epub/11/pg11.txt")
				.body("302 Found"));
		server.expect(requestTo("https://www.gutenberg.org/cache/epub/11/pg11.txt"))
			.andRespond(withSuccess("Actual book text", MediaType.TEXT_PLAIN));

		String content = client.download("http://www.gutenberg.org/cache/epub/11/pg11.txt");

		assertThat(content).isEqualTo("Actual book text");
		server.verify();
	}

	@Test
	void downloadRejectsRedirectWithoutLocation() {
		RestClient.Builder builder = RestClient.builder();
		MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
		GutendexContentClient client = new GutendexContentClient(builder.build());

		server.expect(requestTo("https://www.gutenberg.org/cache/epub/11/pg11.txt"))
			.andRespond(withStatus(HttpStatus.FOUND).body("302 Found"));

		assertThatThrownBy(() -> client.download("https://www.gutenberg.org/cache/epub/11/pg11.txt"))
			.isInstanceOf(ExternalServiceUnavailableException.class)
			.hasMessage("Gutendex content is currently unavailable");
		server.verify();
	}
}
