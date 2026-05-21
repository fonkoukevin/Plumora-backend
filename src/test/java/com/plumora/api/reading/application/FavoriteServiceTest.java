package com.plumora.api.reading.application;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.plumora.api.book.domain.Book;
import com.plumora.api.book.domain.BookStatus;
import com.plumora.api.book.domain.BookVisibility;
import com.plumora.api.book.infrastructure.BookRepository;
import com.plumora.api.reading.infrastructure.FavoriteRepository;
import com.plumora.api.shared.exception.BusinessException;
import com.plumora.api.shared.exception.DuplicateResourceException;
import com.plumora.api.user.application.UserService;
import com.plumora.api.user.domain.User;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FavoriteServiceTest {

	@Mock
	private BookRepository bookRepository;

	@Mock
	private FavoriteRepository favoriteRepository;

	@Mock
	private UserService userService;

	private FavoriteService favoriteService;

	@BeforeEach
	void setUp() {
		favoriteService = new FavoriteService(bookRepository, favoriteRepository, userService);
	}

	@Test
	void addFavoriteRejectsDuplicateFavorite() {
		User reader = user("reader@example.com");
		Book book = publishedBook(user("author@example.com"));

		when(userService.getCurrentUser(reader.getEmail())).thenReturn(reader);
		when(bookRepository.findByIdWithAuthor(book.getId())).thenReturn(Optional.of(book));
		when(favoriteRepository.existsByUserAndBook(reader, book)).thenReturn(true);

		assertThatThrownBy(() -> favoriteService.addFavorite(reader.getEmail(), book.getId()))
			.isInstanceOf(DuplicateResourceException.class)
			.hasMessage("Book is already in favorites");
	}

	@Test
	void addFavoriteRequiresPublishedPublicBook() {
		User reader = user("reader@example.com");
		Book book = publishedBook(user("author@example.com"));
		book.setStatus(BookStatus.DRAFT);
		book.setPublishedAt(null);

		when(userService.getCurrentUser(reader.getEmail())).thenReturn(reader);
		when(bookRepository.findByIdWithAuthor(book.getId())).thenReturn(Optional.of(book));

		assertThatThrownBy(() -> favoriteService.addFavorite(reader.getEmail(), book.getId()))
			.isInstanceOf(BusinessException.class)
			.hasMessage("Only published public books can be added to favorites");
	}

	private Book publishedBook(User author) {
		Book book = new Book();
		book.setId(UUID.randomUUID());
		book.setAuthor(author);
		book.setTitle("Published book");
		book.setGenre("Fantasy");
		book.setStatus(BookStatus.PUBLISHED);
		book.setVisibility(BookVisibility.PUBLIC);
		book.setPublishedAt(LocalDateTime.now());
		return book;
	}

	private User user(String email) {
		User user = new User();
		user.setId(UUID.randomUUID());
		user.setEmail(email);
		user.setUsername(email.substring(0, email.indexOf('@')));
		return user;
	}
}
