package com.plumora.api.book.infrastructure.gutenberg;

import com.plumora.api.shared.exception.ExternalServiceUnavailableException;
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

/**
 * Downloads Project Gutenberg's own official catalog CSV directly from gutenberg.org - never
 * from gutendex.com, which is blocked by Cloudflare from the production VPS (confirmed: 403
 * with a managed challenge page, even from GitHub Actions runners, so it isn't specific to the
 * VPS's own IP). gutenberg.org itself is unaffected. See GutenbergCatalogSyncService for how
 * this feeds the local catalog used for discovery and reading.
 */
@Component
public class GutenbergCatalogClient {

	private static final Logger log = LoggerFactory.getLogger(GutenbergCatalogClient.class);

	private final RestClient restClient;

	@Autowired
	public GutenbergCatalogClient(
		@Value("${app.external.gutenberg.catalog-url:https://www.gutenberg.org/cache/epub/feeds/pg_catalog.csv}") String catalogUrl
	) {
		this(createRestClient(catalogUrl));
	}

	GutenbergCatalogClient(RestClient restClient) {
		this.restClient = restClient;
	}

	public String downloadCatalogCsv() {
		try {
			String body = restClient.get().retrieve().body(String.class);
			if (!StringUtils.hasText(body)) {
				throw unavailable("Gutenberg catalog download returned an empty body", null);
			}
			return body;
		} catch (RestClientResponseException exception) {
			throw unavailable("Gutenberg catalog download failed with status " + exception.getStatusCode().value(), exception);
		} catch (ResourceAccessException exception) {
			throw unavailable("Gutenberg catalog download timed out or could not be reached", exception);
		} catch (RestClientException exception) {
			throw unavailable("Gutenberg catalog download failed", exception);
		}
	}

	private static RestClient createRestClient(String catalogUrl) {
		SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
		requestFactory.setConnectTimeout(10_000);
		// The catalog CSV is ~21MB - a plain search/content timeout would be far too tight.
		requestFactory.setReadTimeout(120_000);
		return RestClient.builder()
			.baseUrl(catalogUrl)
			.requestFactory(requestFactory)
			.build();
	}

	private ExternalServiceUnavailableException unavailable(String message, Exception exception) {
		log.warn(message, exception);
		return new ExternalServiceUnavailableException("Gutenberg catalog is currently unavailable");
	}
}
