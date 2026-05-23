package com.plumora.api.notification.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.plumora.api.notification.domain.Notification;
import com.plumora.api.notification.domain.NotificationType;
import com.plumora.api.notification.infrastructure.NotificationRepository;
import com.plumora.api.shared.exception.UnauthorizedActionException;
import com.plumora.api.user.application.UserService;
import com.plumora.api.user.domain.Role;
import com.plumora.api.user.domain.RoleName;
import com.plumora.api.user.domain.User;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

	@Mock
	private NotificationRepository notificationRepository;

	@Mock
	private UserService userService;

	private NotificationService notificationService;

	@BeforeEach
	void setUp() {
		notificationService = new NotificationService(notificationRepository, userService);
	}

	@Test
	void createNotificationStartsUnread() {
		User user = user("reader@example.com");
		when(notificationRepository.save(org.mockito.ArgumentMatchers.any(Notification.class)))
			.thenAnswer(invocation -> {
				Notification notification = invocation.getArgument(0);
				notification.setId(UUID.randomUUID());
				return notification;
			});

		Notification notification = notificationService.createNotification(
			user,
			"Bienvenue",
			"Votre compte est pret.",
			NotificationType.SYSTEM
		);

		assertThat(notification.getUser()).isEqualTo(user);
		assertThat(notification.isRead()).isFalse();
		assertThat(notification.getReadAt()).isNull();
		assertThat(notification.getType()).isEqualTo(NotificationType.SYSTEM);
	}

	@Test
	void userCanListOwnNotificationsAndUnreadCount() {
		User user = user("reader@example.com");
		Notification notification = notification(user, false);

		when(userService.getCurrentUser(user.getEmail())).thenReturn(user);
		when(notificationRepository.findByUserOrderByCreatedAtDesc(user)).thenReturn(List.of(notification));
		when(notificationRepository.countByUserAndReadFalse(user)).thenReturn(1L);

		assertThat(notificationService.getMyNotifications(user.getEmail())).containsExactly(notification);
		assertThat(notificationService.getUnreadCount(user.getEmail())).isEqualTo(1L);
	}

	@Test
	void ownerCanMarkNotificationAsRead() {
		User user = user("reader@example.com");
		Notification notification = notification(user, false);

		when(notificationRepository.findByIdWithUser(notification.getId())).thenReturn(Optional.of(notification));
		when(notificationRepository.save(notification)).thenReturn(notification);

		Notification updated = notificationService.markAsRead(user.getEmail(), notification.getId());

		assertThat(updated.isRead()).isTrue();
		assertThat(updated.getReadAt()).isNotNull();
	}

	@Test
	void anotherUserCannotMarkNotificationAsRead() {
		User user = user("reader@example.com");
		Notification notification = notification(user, false);

		when(notificationRepository.findByIdWithUser(notification.getId())).thenReturn(Optional.of(notification));

		assertThatThrownBy(() -> notificationService.markAsRead("other@example.com", notification.getId()))
			.isInstanceOf(UnauthorizedActionException.class)
			.hasMessage("Only the notification owner can access this notification");
	}

	@Test
	void markAllAsReadOnlyUpdatesUnreadNotifications() {
		User user = user("reader@example.com");
		Notification unread = notification(user, false);
		Notification read = notification(user, true);

		when(userService.getCurrentUser(user.getEmail())).thenReturn(user);
		when(notificationRepository.findByUserAndReadFalse(user)).thenReturn(List.of(unread));
		when(notificationRepository.findByUserOrderByCreatedAtDesc(user)).thenReturn(List.of(unread, read));

		List<Notification> notifications = notificationService.markAllAsRead(user.getEmail());

		assertThat(unread.isRead()).isTrue();
		assertThat(unread.getReadAt()).isNotNull();
		assertThat(notifications).containsExactly(unread, read);
		verify(notificationRepository).saveAll(List.of(unread));
	}

	@Test
	void ownerCanDeleteNotification() {
		User user = user("reader@example.com");
		Notification notification = notification(user, false);

		when(notificationRepository.findByIdWithUser(notification.getId())).thenReturn(Optional.of(notification));

		notificationService.deleteNotification(user.getEmail(), notification.getId());

		verify(notificationRepository).delete(notification);
	}

	private Notification notification(User user, boolean read) {
		Notification notification = new Notification();
		notification.setId(UUID.randomUUID());
		notification.setUser(user);
		notification.setTitle("Notification");
		notification.setMessage("Message");
		notification.setType(NotificationType.SYSTEM);
		notification.setRead(read);
		notification.setCreatedAt(LocalDateTime.now());
		if (read) {
			notification.setReadAt(LocalDateTime.now());
		}
		return notification;
	}

	private User user(String email) {
		User user = new User();
		user.setId(UUID.randomUUID());
		user.setFirstname("Test");
		user.setLastname("User");
		user.setEmail(email);
		user.setUsername(email.substring(0, email.indexOf('@')));
		Role role = new Role(RoleName.READER, RoleName.READER.name());
		role.setId(UUID.randomUUID());
		user.setRoles(Set.of(role));
		return user;
	}
}
