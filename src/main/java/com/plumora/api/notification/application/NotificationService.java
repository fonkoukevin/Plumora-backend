package com.plumora.api.notification.application;

import com.plumora.api.notification.domain.Notification;
import com.plumora.api.notification.domain.NotificationType;
import com.plumora.api.notification.infrastructure.NotificationRepository;
import com.plumora.api.user.domain.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationService {

	private final NotificationRepository notificationRepository;

	public NotificationService(NotificationRepository notificationRepository) {
		this.notificationRepository = notificationRepository;
	}

	@Transactional
	public Notification createNotification(User user, String title, String message, NotificationType type) {
		Notification notification = new Notification();
		notification.setUser(user);
		notification.setTitle(title);
		notification.setMessage(message);
		notification.setType(type);
		return notificationRepository.save(notification);
	}
}
