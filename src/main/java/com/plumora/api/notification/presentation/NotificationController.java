package com.plumora.api.notification.presentation;

import com.plumora.api.notification.application.NotificationService;
import java.security.Principal;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/notifications")
@PreAuthorize("isAuthenticated()")
public class NotificationController {

	private final NotificationService notificationService;

	public NotificationController(NotificationService notificationService) {
		this.notificationService = notificationService;
	}

	@GetMapping("/my")
	public List<NotificationResponse> getMyNotifications(Principal principal) {
		return notificationService.getMyNotifications(principal.getName())
			.stream()
			.map(NotificationMapper::toResponse)
			.toList();
	}

	@GetMapping("/unread-count")
	public UnreadCountResponse getUnreadCount(Principal principal) {
		return new UnreadCountResponse(notificationService.getUnreadCount(principal.getName()));
	}

	@PatchMapping("/{notificationId}/read")
	public NotificationResponse markAsRead(Principal principal, @PathVariable UUID notificationId) {
		return NotificationMapper.toResponse(notificationService.markAsRead(principal.getName(), notificationId));
	}

	@PatchMapping("/read-all")
	public List<NotificationResponse> markAllAsRead(Principal principal) {
		return notificationService.markAllAsRead(principal.getName())
			.stream()
			.map(NotificationMapper::toResponse)
			.toList();
	}

	@DeleteMapping("/{notificationId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void deleteNotification(Principal principal, @PathVariable UUID notificationId) {
		notificationService.deleteNotification(principal.getName(), notificationId);
	}
}
