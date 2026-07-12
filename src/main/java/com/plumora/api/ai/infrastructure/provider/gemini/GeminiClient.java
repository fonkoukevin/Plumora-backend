package com.plumora.api.ai.infrastructure.provider.gemini;

import com.plumora.api.shared.exception.AiConfigurationException;
import com.plumora.api.shared.exception.AiInvalidResponseException;
import com.plumora.api.shared.exception.AiProviderUnavailableException;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

@Component
public class GeminiClient {

	private static final Logger log = LoggerFactory.getLogger(GeminiClient.class);
	private static final double DEFAULT_TEMPERATURE = 0.4;

	private final RestClient restClient;
	private final String apiKey;
	private final String model;

	@Autowired
	public GeminiClient(
		@Value("${app.external.gemini.base-url:https://generativelanguage.googleapis.com}") String baseUrl,
		@Value("${app.external.gemini.api-key:}") String apiKey,
		@Value("${app.external.gemini.model:gemini-2.5-flash-lite}") String model,
		@Value("${app.external.gemini.timeout-seconds:30}") int timeoutSeconds
	) {
		this(createRestClient(baseUrl, timeoutSeconds), apiKey, model);
	}

	GeminiClient(RestClient restClient, String apiKey, String model) {
		this.restClient = restClient;
		this.apiKey = apiKey;
		this.model = model;
	}

	public String generateJson(String systemPrompt, String userPrompt) {
		if (!StringUtils.hasText(apiKey)) {
			throw new AiConfigurationException(
				"Plumo IA n'est pas configure : GEMINI_API_KEY est absent."
			);
		}

		try {
			GeminiGenerateContentResponse response = restClient.post()
				.uri(uriBuilder -> uriBuilder
					.path("/v1beta/models/{model}:generateContent")
					.queryParam("key", apiKey)
					.build(model))
				.body(new GeminiGenerateContentRequest(
					new GeminiSystemInstruction(List.of(new GeminiPart(systemPrompt))),
					List.of(new GeminiContent("user", List.of(new GeminiPart(userPrompt)))),
					new GeminiGenerationConfig("application/json", DEFAULT_TEMPERATURE)
				))
				.retrieve()
				.body(GeminiGenerateContentResponse.class);
			return extractText(response);
		} catch (RestClientResponseException exception) {
			throw unavailable("Gemini call failed with status " + exception.getStatusCode().value(), exception);
		} catch (ResourceAccessException exception) {
			throw unavailable("Gemini call timed out or could not be reached", exception);
		} catch (RestClientException exception) {
			throw unavailable("Gemini call failed", exception);
		}
	}

	public String model() {
		return model;
	}

	private String extractText(GeminiGenerateContentResponse response) {
		if (response == null || response.candidates() == null || response.candidates().isEmpty()) {
			throw new AiInvalidResponseException("Gemini returned no candidates");
		}
		GeminiContent content = response.candidates().get(0).content();
		if (content == null || content.parts() == null || content.parts().isEmpty()) {
			throw new AiInvalidResponseException("Gemini returned an empty response");
		}
		String text = content.parts().get(0).text();
		if (!StringUtils.hasText(text)) {
			throw new AiInvalidResponseException("Gemini returned an empty response");
		}
		return text;
	}

	private static RestClient createRestClient(String baseUrl, int timeoutSeconds) {
		SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
		requestFactory.setConnectTimeout(5000);
		requestFactory.setReadTimeout(Math.max(timeoutSeconds, 1) * 1000);
		return RestClient.builder()
			.baseUrl(baseUrl)
			.requestFactory(requestFactory)
			.build();
	}

	private AiProviderUnavailableException unavailable(String message, Exception exception) {
		log.warn(message, exception);
		return new AiProviderUnavailableException("Plumo IA (Gemini) is currently unavailable");
	}
}
