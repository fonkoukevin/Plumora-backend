package com.plumora.api.reading.application;

import com.plumora.api.book.domain.Book;
import com.plumora.api.book.domain.BookStatus;
import com.plumora.api.book.domain.BookVisibility;
import com.plumora.api.book.infrastructure.BookRepository;
import com.plumora.api.reading.domain.Favorite;
import com.plumora.api.reading.infrastructure.FavoriteRepository;
import com.plumora.api.shared.exception.BusinessException;
import com.plumora.api.shared.exception.DuplicateResourceException;
import com.plumora.api.shared.exception.ResourceNotFoundException;
import com.plumora.api.user.application.UserService;
import com.plumora.api.user.domain.User;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FavoriteService {

	private final BookRepository bookRepository;
	private final FavoriteRepository favoriteRepository;
	private final UserService userService;

	public FavoriteService(
		BookRepository bookRepository,
		FavoriteRepository favoriteRepository,
		UserService userService
	) {
		this.bookRepository = bookRepository;
		this.favoriteRepository = favoriteRepository;
		this.userService = userService;
	}

	@Transactional
	public Favorite addFavorite(String currentUserEmail, UUID bookId) {
		User user = userService.getCurrentUser(currentUserEmail);
		Book book = getPublishedPublicBook(bookId);
		if (favoriteRepository.existsByUserAndBook(user, book)) {
			throw new DuplicateResourceException("Book is already in favorites");
		}
		Favorite favorite = new Favorite();
		favorite.setUser(user);
		favorite.setBook(book);
		return favoriteRepository.save(favorite);
	}

	@Transactional
	public void removeFavorite(String currentUserEmail, UUID bookId) {
		User user = userService.getCurrentUser(currentUserEmail);
		Book book = getBook(bookId);
		Favorite favorite = favoriteRepository.findByUserAndBook(user, book)
			.orElseThrow(() -> new ResourceNotFoundException("Favorite was not found"));
		favoriteRepository.delete(favorite);
	}

	@Transactional(readOnly = true)
	public List<Favorite> getMyFavorites(String currentUserEmail) {
		User user = userService.getCurrentUser(currentUserEmail);
		return favoriteRepository.findByUserOrderByCreatedAtDesc(user);
	}

	@Transactional(readOnly = true)
	public boolean isFavorite(String currentUserEmail, UUID bookId) {
		User user = userService.getCurrentUser(currentUserEmail);
		Book book = getBook(bookId);
		return favoriteRepository.existsByUserAndBook(user, book);
	}

	private Book getBook(UUID bookId) {
		return bookRepository.findByIdWithAuthor(bookId)
			.orElseThrow(() -> new ResourceNotFoundException("Book was not found"));
	}

	private Book getPublishedPublicBook(UUID bookId) {
		Book book = getBook(bookId);
		if (book.getStatus() != BookStatus.PUBLISHED
			|| book.getVisibility() != BookVisibility.PUBLIC
			|| book.getPublishedAt() == null) {
			throw new BusinessException("Only published public books can be added to favorites");
		}
		return book;
	}
}
