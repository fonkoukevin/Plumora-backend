package com.plumora.api.book.infrastructure.openlibrary;

import com.plumora.api.shared.exception.ExternalServiceUnavailableException;
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

/**
 * Discovery-only fallback used when Gutendex itself is unreachable (see ExternalBookService).
 * Open Library is a metadata catalog, not a full-text source like Gutendex: results from here
 * never carry a readUrl/formats, so they are never importable/readable in-app, only browsable.
 */
@Component
public class OpenLibraryClient {

	private static final Logger log = LoggerFactory.getLogger(OpenLibraryClient.class);
	private static final String SEARCH_FIELDS = "key,title,author_name,cover_i,isbn,subject";
	private static final int PAGE_SIZE = 32;

	private final RestClient restClient;

	@Autowired
	public OpenLibraryClient(@Value("${app.external.open-library.base-url:https://openlibrary.org}") String baseUrl) {
		this(createRestClient(baseUrl));
	}

	OpenLibraryClient(RestClient restClient) {
		this.restClient = restClient;
	}

	public OpenLibrarySearchResponse searchBooks(String search, String subject, int page) {
		try {
			OpenLibrarySearchResponse response = restClient.get()
				.uri(uriBuilder -> {
					uriBuilder.path("/search.json")
						.queryParam("fields", SEARCH_FIELDS)
						.queryParam("limit", PAGE_SIZE)
						.queryParam("page", Math.max(page, 1))
						.queryParam("q", query(search, subject));
					return uriBuilder.build();
				})
				.retrieve()
				.body(OpenLibrarySearchResponse.class);
			return response == null ? empty() : response;
		} catch (RestClientResponseException exception) {
			throw unavailable("Open Library search failed with status " + exception.getStatusCode().value(), exception);
		} catch (ResourceAccessException exception) {
			throw unavailable("Open Library search timed out or could not be reached", exception);
		} catch (RestClientException exception) {
			throw unavailable("Open Library search failed", exception);
		}
	}

	private String query(String search, String subject) {
		StringBuilder query = new StringBuilder();
		if (StringUtils.hasText(search)) {
			query.append(search.trim());
		}
		if (StringUtils.hasText(subject)) {
			if (!query.isEmpty()) {
				query.append(' ');
			}
			query.append("subject:").append(subject.trim());
		}
		// Open Library rejects a bare "q=*" wildcard with 422, and an entirely omitted "q"
		// returns zero results (both confirmed against the real API) - default to a broad
		// subject so discovery sections without an explicit filter (e.g. "Tendances"/
		// "Nouveautes") still show real, browsable books instead of an empty list.
		return query.isEmpty() ? "subject:fiction" : query.toString();
	}

	private OpenLibrarySearchResponse empty() {
		return new OpenLibrarySearchResponse(0, List.of());
	}

	private static RestClient createRestClient(String baseUrl) {
		SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
		requestFactory.setConnectTimeout(3000);
		// The default (no search/subject) fallback query matches millions of documents, which
		// is measurably slower for Open Library to paginate than a narrow subject query - 5s
		// was intermittently too tight for it in production, causing spurious 503s.
		requestFactory.setReadTimeout(10000);
		return RestClient.builder()
			.baseUrl(baseUrl)
			.requestFactory(requestFactory)
			.build();
	}

	private ExternalServiceUnavailableException unavailable(String message, Exception exception) {
		log.warn(message, exception);
		return new ExternalServiceUnavailableException("Open Library is currently unavailable");
	}
}
