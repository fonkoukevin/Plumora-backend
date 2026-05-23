package com.plumora.api.notification.presentation;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.plumora.api.notification.domain.NotificationType;
import java.time.LocalDateTime;
import java.util.UUID;

public record NotificationResponse(
	UUID id,
	String title,
	String message,
	NotificationType type,
	@JsonProperty("is_read")
	boolean read,
	@JsonProperty("created_at")
	LocalDateTime createdAt,
	@JsonProperty("read_at")
	LocalDateTime readAt
) {
}
