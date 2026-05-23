package com.plumora.api.notification.presentation;

import com.plumora.api.notification.domain.Notification;

public final class NotificationMapper {
	private NotificationMapper() {
	}

	public static NotificationResponse toResponse(Notification notification) {
		return new NotificationResponse(
			notification.getId(),
			notification.getTitle(),
			notification.getMessage(),
			notification.getType(),
			notification.isRead(),
			notification.getCreatedAt(),
			notification.getReadAt()
		);
	}
}
