package com.plumora.api.book.infrastructure.gutendex;

import com.plumora.api.shared.exception.BusinessException;
import com.plumora.api.shared.exception.ExternalServiceUnavailableException;
import java.net.URI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

@Component
public class GutendexContentClient {

	private static final Logger log = LoggerFactory.getLogger(GutendexContentClient.class);
	private static final int MAX_REDIRECTS = 5;

	private final RestClient restClient;

	public GutendexContentClient() {
		this(createRestClient());
	}

	GutendexContentClient(RestClient restClient) {
		this.restClient = restClient;
	}

	public String download(String url) {
		URI uri = uri(url);
		return download(uri, 0);
	}

	private String download(URI uri, int redirectCount) {
		try {
			ResponseEntity<String> response = restClient.get()
				.uri(uri)
				.retrieve()
				.toEntity(String.class);
			if (response.getStatusCode().is3xxRedirection()) {
				return followRedirect(uri, response, redirectCount);
			}
			return response.getBody() == null ? "" : response.getBody();
		} catch (RestClientResponseException exception) {
			log.warn("Gutendex content download failed with status {}", exception.getStatusCode().value());
			throw new ExternalServiceUnavailableException("Gutendex content is currently unavailable");
		} catch (ResourceAccessException exception) {
			log.warn("Gutendex content download timed out or could not be reached");
			throw new ExternalServiceUnavailableException("Gutendex content is currently unavailable");
		} catch (RestClientException exception) {
			log.warn("Gutendex content download failed", exception);
			throw new ExternalServiceUnavailableException("Gutendex content is currently unavailable");
		}
	}

	private String followRedirect(URI currentUri, ResponseEntity<String> response, int redirectCount) {
		if (redirectCount >= MAX_REDIRECTS) {
			throw new ExternalServiceUnavailableException("Gutendex content is currently unavailable");
		}
		URI location = response.getHeaders().getLocation();
		if (location == null) {
			throw new ExternalServiceUnavailableException("Gutendex content is currently unavailable");
		}
		URI redirectUri = checkedUri(currentUri.resolve(location));
		return download(redirectUri, redirectCount + 1);
	}

	private URI uri(String url) {
		if (!StringUtils.hasText(url)) {
			throw new BusinessException("No readable Gutendex content URL is available");
		}
		return checkedUri(URI.create(url.trim()));
	}

	private URI checkedUri(URI uri) {
		if (!"http".equalsIgnoreCase(uri.getScheme()) && !"https".equalsIgnoreCase(uri.getScheme())) {
			throw new BusinessException("Readable Gutendex content URL must be HTTP or HTTPS");
		}
		return uri;
	}

	private static RestClient createRestClient() {
		SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
		requestFactory.setConnectTimeout(3000);
		requestFactory.setReadTimeout(10000);
		return RestClient.builder()
			.requestFactory(requestFactory)
			.build();
	}
}
