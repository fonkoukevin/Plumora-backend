package com.plumora.api.reading.presentation;

import com.plumora.api.reading.application.FavoriteService;
import java.security.Principal;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping
public class FavoriteController {

	private final FavoriteService favoriteService;

	public FavoriteController(FavoriteService favoriteService) {
		this.favoriteService = favoriteService;
	}

	@PostMapping("/books/{bookId}/favorites")
	@ResponseStatus(HttpStatus.CREATED)
	@PreAuthorize("hasRole('READER')")
	public FavoriteResponse addFavorite(Principal principal, @PathVariable UUID bookId) {
		return ReadingMapper.toFavoriteResponse(favoriteService.addFavorite(principal.getName(), bookId));
	}

	@DeleteMapping("/books/{bookId}/favorites")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@PreAuthorize("hasRole('READER')")
	public void removeFavorite(Principal principal, @PathVariable UUID bookId) {
		favoriteService.removeFavorite(principal.getName(), bookId);
	}

	@GetMapping("/favorites/my")
	@PreAuthorize("hasRole('READER')")
	public List<FavoriteResponse> getMyFavorites(Principal principal) {
		return favoriteService.getMyFavorites(principal.getName())
			.stream()
			.map(ReadingMapper::toFavoriteResponse)
			.toList();
	}

	@GetMapping("/books/{bookId}/favorites/status")
	@PreAuthorize("hasRole('READER')")
	public FavoriteStatusResponse getFavoriteStatus(Principal principal, @PathVariable UUID bookId) {
		return new FavoriteStatusResponse(bookId, favoriteService.isFavorite(principal.getName(), bookId));
	}
}
