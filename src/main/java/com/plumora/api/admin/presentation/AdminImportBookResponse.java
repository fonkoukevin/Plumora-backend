package com.plumora.api.admin.presentation;

import java.util.UUID;

public record AdminImportBookResponse(
	UUID bookId,
	String title,
	String source,
	String externalId,
	boolean imported,
	boolean alreadyExisted,
	String message
) {
}
