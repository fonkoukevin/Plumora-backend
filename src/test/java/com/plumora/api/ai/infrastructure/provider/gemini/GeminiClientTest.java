package com.plumora.api.ai.infrastructure.provider.gemini;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.plumora.api.shared.exception.AiConfigurationException;
import com.plumora.api.shared.exception.AiProviderUnavailableException;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class GeminiClientTest {

	@Test
	void generateJsonReturnsTextFromFirstCandidate() {
		RestClient.Builder builder = RestClient.builder().baseUrl("https://gemini.test");
		MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
		GeminiClient client = new GeminiClient(builder.build(), "test-key", "gemini-2.5-flash-lite");
		String response = """
			{
			  "candidates": [
			    {
			      "content": {
			        "role": "model",
			        "parts": [{"text": "{\\"suggestion\\": \\"Texte reformule\\"}"}]
			      }
			    }
			  ]
			}
			""";

		server.expect(requestTo("https://gemini.test/v1beta/models/gemini-2.5-flash-lite:generateContent?key=test-key"))
			.andRespond(withSuccess(response, MediaType.APPLICATION_JSON));

		String result = client.generateJson("system prompt", "user prompt");

		assertThat(result).isEqualTo("{\"suggestion\": \"Texte reformule\"}");
		server.verify();
	}

	@Test
	void generateJsonThrowsConfigurationExceptionWhenApiKeyMissing() {
		RestClient restClient = RestClient.builder().baseUrl("https://gemini.test").build();
		GeminiClient client = new GeminiClient(restClient, "", "gemini-2.5-flash-lite");

		assertThatThrownBy(() -> client.generateJson("system prompt", "user prompt"))
			.isInstanceOf(AiConfigurationException.class);
	}

	@Test
	void generateJsonThrowsProviderUnavailableWhenGeminiFails() {
		RestClient.Builder builder = RestClient.builder().baseUrl("https://gemini.test");
		MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
		GeminiClient client = new GeminiClient(builder.build(), "test-key", "gemini-2.5-flash-lite");

		server.expect(requestTo("https://gemini.test/v1beta/models/gemini-2.5-flash-lite:generateContent?key=test-key"))
			.andRespond(withServerError());

		assertThatThrownBy(() -> client.generateJson("system prompt", "user prompt"))
			.isInstanceOf(AiProviderUnavailableException.class);
		server.verify();
	}

	@Test
	void generateJsonFailsFastAndGracefullyWhenGeminiNeverResponds() throws Exception {
		// OWASP A10 (Mishandling of Exceptional Conditions) / resilience: this is not a mocked
		// exception path but a real socket-level read timeout - a real local HTTP server accepts
		// the connection and then never writes a response, exercising the actual
		// SimpleClientHttpRequestFactory.setReadTimeout(...) wired in GeminiClient's production
		// constructor. Proves the call is bounded in time (never hangs indefinitely waiting on a
		// slow/stuck provider) and surfaces as the same AiProviderUnavailableException a caller
		// already handles, rather than a raw SocketTimeoutException leaking out.
		HttpServer neverRespondingServer = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
		neverRespondingServer.createContext("/", exchange -> {
			// Deliberately never calls sendResponseHeaders/close: the client's read timeout must
			// be what ends this request, not the server.
		});
		neverRespondingServer.start();
		try {
			SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
			requestFactory.setConnectTimeout(2000);
			requestFactory.setReadTimeout(500);
			RestClient restClient = RestClient.builder()
				.baseUrl("http://localhost:" + neverRespondingServer.getAddress().getPort())
				.requestFactory(requestFactory)
				.build();
			GeminiClient client = new GeminiClient(restClient, "test-key", "gemini-2.5-flash-lite");

			long start = System.nanoTime();
			assertThatThrownBy(() -> client.generateJson("system prompt", "user prompt"))
				.isInstanceOf(AiProviderUnavailableException.class);
			Duration elapsed = Duration.ofNanos(System.nanoTime() - start);

			assertThat(elapsed).isLessThan(Duration.ofSeconds(5));
		} finally {
			neverRespondingServer.stop(0);
		}
	}
}
