package com.plumora.api.notification.application;

import com.plumora.api.notification.domain.Notification;
import com.plumora.api.notification.domain.NotificationType;
import com.plumora.api.notification.infrastructure.NotificationRepository;
import com.plumora.api.shared.exception.ResourceNotFoundException;
import com.plumora.api.shared.exception.UnauthorizedActionException;
import com.plumora.api.user.application.UserService;
import com.plumora.api.user.domain.User;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationService {

	private final NotificationRepository notificationRepository;
	private final UserService userService;

	public NotificationService(NotificationRepository notificationRepository, UserService userService) {
		this.notificationRepository = notificationRepository;
		this.userService = userService;
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

	@Transactional(readOnly = true)
	public List<Notification> getMyNotifications(String currentUserEmail) {
		User currentUser = userService.getCurrentUser(currentUserEmail);
		return notificationRepository.findByUserOrderByCreatedAtDesc(currentUser);
	}

	@Transactional(readOnly = true)
	public long getUnreadCount(String currentUserEmail) {
		User currentUser = userService.getCurrentUser(currentUserEmail);
		return notificationRepository.countByUserAndReadFalse(currentUser);
	}

	@Transactional
	public Notification markAsRead(String currentUserEmail, UUID notificationId) {
		Notification notification = findNotification(notificationId);
		ensureOwner(currentUserEmail, notification);
		markAsRead(notification, LocalDateTime.now());
		return notificationRepository.save(notification);
	}

	@Transactional
	public List<Notification> markAllAsRead(String currentUserEmail) {
		User currentUser = userService.getCurrentUser(currentUserEmail);
		LocalDateTime now = LocalDateTime.now();
		List<Notification> unreadNotifications = notificationRepository.findByUserAndReadFalse(currentUser);
		unreadNotifications.forEach(notification -> markAsRead(notification, now));
		notificationRepository.saveAll(unreadNotifications);
		return notificationRepository.findByUserOrderByCreatedAtDesc(currentUser);
	}

	@Transactional
	public void deleteNotification(String currentUserEmail, UUID notificationId) {
		Notification notification = findNotification(notificationId);
		ensureOwner(currentUserEmail, notification);
		notificationRepository.delete(notification);
	}

	private Notification findNotification(UUID notificationId) {
		return notificationRepository.findByIdWithUser(notificationId)
			.orElseThrow(() -> new ResourceNotFoundException("Notification was not found"));
	}

	private void markAsRead(Notification notification, LocalDateTime readAt) {
		if (!notification.isRead()) {
			notification.setRead(true);
			notification.setReadAt(readAt);
		}
	}

	private void ensureOwner(String currentUserEmail, Notification notification) {
		if (!notification.getUser().getEmail().equals(currentUserEmail)) {
			throw new UnauthorizedActionException("Only the notification owner can access this notification");
		}
	}
}
