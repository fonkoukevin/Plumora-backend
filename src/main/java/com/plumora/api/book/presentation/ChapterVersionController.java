package com.plumora.api.book.presentation;

import com.plumora.api.book.application.ChapterVersionService;
import com.plumora.api.book.application.ChapterVersionService.RestoreResult;
import java.security.Principal;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping
public class ChapterVersionController {

	private final ChapterVersionService chapterVersionService;

	public ChapterVersionController(ChapterVersionService chapterVersionService) {
		this.chapterVersionService = chapterVersionService;
	}

	@PostMapping("/chapters/{chapterId}/versions")
	@ResponseStatus(HttpStatus.CREATED)
	@PreAuthorize("hasRole('AUTHOR')")
	public ChapterVersionResponse createVersion(Principal principal, @PathVariable UUID chapterId) {
		return BookMapper.toResponse(chapterVersionService.createVersion(principal.getName(), chapterId));
	}

	@GetMapping("/chapters/{chapterId}/versions")
	@PreAuthorize("hasRole('AUTHOR')")
	public List<ChapterVersionResponse> getChapterVersions(Principal principal, @PathVariable UUID chapterId) {
		return chapterVersionService.getChapterVersions(principal.getName(), chapterId)
			.stream()
			.map(BookMapper::toResponse)
			.toList();
	}

	@GetMapping("/chapter-versions/{versionId}")
	@PreAuthorize("hasRole('AUTHOR')")
	public ChapterVersionResponse getVersion(Principal principal, @PathVariable UUID versionId) {
		return BookMapper.toResponse(chapterVersionService.getVersion(principal.getName(), versionId));
	}

	@PostMapping("/chapter-versions/{versionId}/restore")
	@PreAuthorize("hasRole('AUTHOR')")
	public RestoreChapterVersionResponse restoreVersion(Principal principal, @PathVariable UUID versionId) {
		RestoreResult result = chapterVersionService.restoreVersion(principal.getName(), versionId);
		return new RestoreChapterVersionResponse(
			BookMapper.toResponse(result.chapter()),
			BookMapper.toResponse(result.restoredVersion()),
			result.backupVersion() == null ? null : BookMapper.toResponse(result.backupVersion())
		);
	}
}
