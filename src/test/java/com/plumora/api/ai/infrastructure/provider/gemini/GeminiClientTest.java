package com.plumora.api.ai.infrastructure.provider.gemini;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.plumora.api.shared.exception.AiConfigurationException;
import com.plumora.api.shared.exception.AiProviderUnavailableException;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
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
}
