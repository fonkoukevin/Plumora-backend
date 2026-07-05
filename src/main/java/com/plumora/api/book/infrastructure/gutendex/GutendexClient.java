package com.plumora.api.book.infrastructure.gutendex;

import com.plumora.api.shared.exception.ExternalServiceUnavailableException;
import java.util.Optional;
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
public class GutendexClient {

	private static final Logger log = LoggerFactory.getLogger(GutendexClient.class);

	private final RestClient restClient;

	@Autowired
	public GutendexClient(@Value("${app.external.gutendex.base-url:https://gutendex.com}") String baseUrl) {
		this(createRestClient(baseUrl));
	}

	GutendexClient(RestClient restClient) {
		this.restClient = restClient;
	}

	public GutendexPageResponse searchBooks(GutendexSearchRequest request) {
		try {
			GutendexPageResponse response = restClient.get()
				.uri(uriBuilder -> {
					uriBuilder.path("/books")
						.queryParam("page", request.page())
						.queryParam("sort", request.sort())
						.queryParam("copyright", request.copyright());
					addIfPresent(uriBuilder, "search", request.search());
					addIfPresent(uriBuilder, "languages", request.language());
					addIfPresent(uriBuilder, "topic", request.topic());
					return uriBuilder.build();
				})
				.retrieve()
				.body(GutendexPageResponse.class);
			return response == null ? GutendexPageResponse.empty() : response;
		} catch (RestClientResponseException exception) {
			throw unavailable("Gutendex search failed with status " + exception.getStatusCode().value(), exception);
		} catch (ResourceAccessException exception) {
			throw unavailable("Gutendex search timed out or could not be reached", exception);
		} catch (RestClientException exception) {
			throw unavailable("Gutendex search failed", exception);
		}
	}

	public Optional<GutendexBookResponse> getBook(int gutendexId) {
		try {
			GutendexBookResponse response = restClient.get()
				.uri("/books/{id}", gutendexId)
				.retrieve()
				.body(GutendexBookResponse.class);
			return Optional.ofNullable(response);
		} catch (RestClientResponseException exception) {
			if (exception.getStatusCode().value() == 404) {
				return Optional.empty();
			}
			throw unavailable("Gutendex detail failed with status " + exception.getStatusCode().value(), exception);
		} catch (ResourceAccessException exception) {
			throw unavailable("Gutendex detail timed out or could not be reached", exception);
		} catch (RestClientException exception) {
			throw unavailable("Gutendex detail failed", exception);
		}
	}

	private static RestClient createRestClient(String baseUrl) {
		SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
		requestFactory.setConnectTimeout(3000);
		requestFactory.setReadTimeout(5000);
		return RestClient.builder()
			.baseUrl(baseUrl)
			.requestFactory(requestFactory)
			.build();
	}

	private static void addIfPresent(org.springframework.web.util.UriBuilder uriBuilder, String name, String value) {
		if (StringUtils.hasText(value)) {
			uriBuilder.queryParam(name, value.trim());
		}
	}

	private ExternalServiceUnavailableException unavailable(String message, Exception exception) {
		log.warn(message, exception);
		return new ExternalServiceUnavailableException("Gutendex is currently unavailable");
	}
}
