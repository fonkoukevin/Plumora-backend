package com.plumora.api.book.infrastructure.openlibrary;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

@Service
public class OpenLibraryCoverService {

	private static final Logger log = LoggerFactory.getLogger(OpenLibraryCoverService.class);
	private static final Duration CACHE_TTL = Duration.ofHours(24);
	private static final String SEARCH_FIELDS = "key,title,author_name,cover_i,isbn";

	private final RestClient restClient;
	private final Clock clock;
	private final Map<String, CacheEntry> cache = new ConcurrentHashMap<>();

	@Autowired
	public OpenLibraryCoverService(
		@Value("${app.external.open-library.base-url:https://openlibrary.org}") String baseUrl
	) {
		this(createRestClient(baseUrl), Clock.systemUTC());
	}

	OpenLibraryCoverService(RestClient restClient, Clock clock) {
		this.restClient = restClient;
		this.clock = clock;
	}

	public String resolveCoverUrl(String title, String author) {
		if (!StringUtils.hasText(title)) {
			return null;
		}

		String cacheKey = cacheKey(title, author);
		CacheEntry cached = cache.get(cacheKey);
		Instant now = Instant.now(clock);
		if (cached != null && cached.expiresAt().isAfter(now)) {
			return cached.coverUrl();
		}

		String coverUrl = fetchCoverUrl(title.trim(), StringUtils.hasText(author) ? author.trim() : null);
		cache.put(cacheKey, new CacheEntry(coverUrl, now.plus(CACHE_TTL)));
		return coverUrl;
	}

	private String fetchCoverUrl(String title, String author) {
		try {
			OpenLibrarySearchResponse response = restClient.get()
				.uri(uriBuilder -> {
					uriBuilder.path("/search.json")
						.queryParam("title", title)
						.queryParam("fields", SEARCH_FIELDS)
						.queryParam("limit", 1);
					if (StringUtils.hasText(author)) {
						uriBuilder.queryParam("author", author);
					}
					return uriBuilder.build();
				})
				.retrieve()
				.body(OpenLibrarySearchResponse.class);
			return coverUrl(response);
		} catch (RestClientResponseException exception) {
			log.warn("Open Library cover search failed with status {}", exception.getStatusCode().value());
			return null;
		} catch (ResourceAccessException exception) {
			log.warn("Open Library cover search timed out or could not be reached");
			return null;
		} catch (RestClientException exception) {
			log.warn("Open Library cover search failed", exception);
			return null;
		}
	}

	private String coverUrl(OpenLibrarySearchResponse response) {
		if (response == null || response.docs() == null || response.docs().isEmpty()) {
			return null;
		}
		OpenLibraryDocResponse doc = response.docs().getFirst();
		if (doc.coverId() != null) {
			return "https://covers.openlibrary.org/b/id/" + doc.coverId() + "-L.jpg?default=false";
		}
		if (doc.isbn() != null) {
			return doc.isbn().stream()
				.filter(StringUtils::hasText)
				.findFirst()
				.map(isbn -> "https://covers.openlibrary.org/b/isbn/" + isbn + "-L.jpg?default=false")
				.orElse(null);
		}
		return null;
	}

	private String cacheKey(String title, String author) {
		String normalizedTitle = title.trim().toLowerCase(Locale.ROOT);
		String normalizedAuthor = StringUtils.hasText(author) ? author.trim().toLowerCase(Locale.ROOT) : "";
		return normalizedTitle + "|" + normalizedAuthor;
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

	private record CacheEntry(String coverUrl, Instant expiresAt) {
	}
}
