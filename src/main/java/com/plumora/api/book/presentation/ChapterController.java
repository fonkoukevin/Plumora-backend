package com.plumora.api.book.presentation;

import com.plumora.api.book.application.ChapterService;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping
public class ChapterController {

	private final ChapterService chapterService;

	public ChapterController(ChapterService chapterService) {
		this.chapterService = chapterService;
	}

	@PostMapping("/books/{bookId}/chapters")
	@ResponseStatus(HttpStatus.CREATED)
	@PreAuthorize("hasRole('AUTHOR')")
	public ChapterResponse createChapter(
		Principal principal,
		@PathVariable UUID bookId,
		@Valid @RequestBody CreateChapterRequest request
	) {
		return BookMapper.toResponse(chapterService.createChapter(principal.getName(), bookId, request));
	}

	@GetMapping("/books/{bookId}/chapters")
	@PreAuthorize("hasRole('AUTHOR')")
	public List<ChapterResponse> getBookChapters(Principal principal, @PathVariable UUID bookId) {
		return chapterService.getBookChapters(principal.getName(), bookId)
			.stream()
			.map(BookMapper::toResponse)
			.toList();
	}

	@GetMapping("/chapters/{chapterId}")
	@PreAuthorize("hasRole('AUTHOR')")
	public ChapterResponse getChapter(Principal principal, @PathVariable UUID chapterId) {
		return BookMapper.toResponse(chapterService.getChapter(principal.getName(), chapterId));
	}

	@PutMapping("/chapters/{chapterId}")
	@PreAuthorize("hasRole('AUTHOR')")
	public ChapterResponse updateChapter(
		Principal principal,
		@PathVariable UUID chapterId,
		@Valid @RequestBody UpdateChapterRequest request
	) {
		return BookMapper.toResponse(chapterService.updateChapter(principal.getName(), chapterId, request));
	}

	@PatchMapping("/chapters/{chapterId}/order")
	@PreAuthorize("hasRole('AUTHOR')")
	public ChapterResponse updateChapterOrder(
		Principal principal,
		@PathVariable UUID chapterId,
		@Valid @RequestBody UpdateChapterOrderRequest request
	) {
		return BookMapper.toResponse(chapterService.updateChapterOrder(principal.getName(), chapterId, request.chapterOrder()));
	}

	@DeleteMapping("/chapters/{chapterId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@PreAuthorize("hasRole('AUTHOR')")
	public void deleteChapter(Principal principal, @PathVariable UUID chapterId) {
		chapterService.deleteChapter(principal.getName(), chapterId);
	}
}
