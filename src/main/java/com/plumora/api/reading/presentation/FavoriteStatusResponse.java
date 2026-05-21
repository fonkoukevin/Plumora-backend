package com.plumora.api.reading.presentation;

import java.util.UUID;

public record FavoriteStatusResponse(
	UUID bookId,
	boolean favorite
) {
}
