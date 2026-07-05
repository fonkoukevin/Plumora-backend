package com.plumora.api.book.presentation;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;
import org.springframework.util.StringUtils;

public final class BookCoverUrlMapper {
	private BookCoverUrlMapper() {
	}

	public static String toResponseCoverUrl(String coverUrl) {
		if (!StringUtils.hasText(coverUrl)) {
			return null;
		}

		String trimmedCoverUrl = coverUrl.trim();
		if (isPlaceholderCoverUrl(trimmedCoverUrl)) {
			return null;
		}

		return trimmedCoverUrl;
	}

	private static boolean isPlaceholderCoverUrl(String coverUrl) {
		try {
			URI uri = new URI(coverUrl);
			String host = uri.getHost();
			if (host == null) {
				return false;
			}
			String normalizedHost = host.toLowerCase(Locale.ROOT);
			return normalizedHost.endsWith("placehold.co")
				|| normalizedHost.endsWith("example.com")
				|| normalizedHost.endsWith("example.org")
				|| normalizedHost.endsWith("example.net");
		} catch (URISyntaxException exception) {
			return false;
		}
	}
}
