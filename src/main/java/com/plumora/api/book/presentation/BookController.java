package com.plumora.api.book.presentation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.plumora.api.book.application.BookService;
import com.plumora.api.book.application.ChapterStats;
import com.plumora.api.book.domain.Book;
import com.plumora.api.shared.exception.BusinessException;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import java.io.IOException;
import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.StringUtils;
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
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

@RestController
@RequestMapping("/books")
public class BookController {

	private final BookService bookService;
	private final ObjectMapper objectMapper;
	private final Validator validator;

	public BookController(BookService bookService, ObjectMapper objectMapper, Validator validator) {
		this.bookService = bookService;
		this.objectMapper = objectMapper;
		this.validator = validator;
	}

	@Operation(requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(content = {
		@Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = CreateBookRequest.class)),
		@Content(mediaType = MediaType.MULTIPART_FORM_DATA_VALUE, schema = @Schema(implementation = BookMultipartRequest.class))
	}))
	@PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
	@ResponseStatus(HttpStatus.CREATED)
	@PreAuthorize("hasRole('AUTHOR')")
	public BookResponse createBook(
		Principal principal,
		@Valid @RequestBody CreateBookRequest request
	) {
		return BookMapper.toResponse(bookService.createBook(principal.getName(), request), ChapterStats.EMPTY);
	}

	@Hidden
	@PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	@ResponseStatus(HttpStatus.CREATED)
	@PreAuthorize("hasRole('AUTHOR')")
	public BookResponse createBookWithCover(
		Principal principal,
		MultipartHttpServletRequest multipartRequest
	) {
		CreateBookRequest request = validate(createBookRequest(multipartRequest));
		return BookMapper.toResponse(bookService.createBook(
			principal.getName(),
			request,
			coverImage(multipartRequest)
		), ChapterStats.EMPTY);
	}

	@GetMapping("/my-books")
	@PreAuthorize("hasRole('AUTHOR')")
	public List<BookResponse> getMyBooks(Principal principal) {
		List<Book> books = bookService.getMyBooks(principal.getName());
		Map<UUID, ChapterStats> statsByBookId = bookService.getChapterStats(books);
		return books.stream()
			.map(book -> BookMapper.toResponse(book, statsByBookId.getOrDefault(book.getId(), ChapterStats.EMPTY)))
			.toList();
	}

	@GetMapping("/{bookId}")
	public BookResponse getBook(@PathVariable UUID bookId) {
		Book book = bookService.getBook(bookId);
		return BookMapper.toResponse(book, bookService.getChapterStats(book));
	}

	@Operation(requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(content = {
		@Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = UpdateBookRequest.class)),
		@Content(mediaType = MediaType.MULTIPART_FORM_DATA_VALUE, schema = @Schema(implementation = BookMultipartRequest.class))
	}))
	@PutMapping(value = "/{bookId}", consumes = MediaType.APPLICATION_JSON_VALUE)
	@PreAuthorize("hasRole('AUTHOR')")
	public BookResponse updateBook(
		Principal principal,
		@PathVariable UUID bookId,
		@Valid @RequestBody UpdateBookRequest request
	) {
		Book book = bookService.updateBook(principal.getName(), bookId, request);
		return BookMapper.toResponse(book, bookService.getChapterStats(book));
	}

	@Hidden
	@PutMapping(value = "/{bookId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	@PreAuthorize("hasRole('AUTHOR')")
	public BookResponse updateBookWithCover(
		Principal principal,
		@PathVariable UUID bookId,
		MultipartHttpServletRequest multipartRequest
	) {
		UpdateBookRequest request = validate(updateBookRequest(multipartRequest));
		Book book = bookService.updateBook(
			principal.getName(),
			bookId,
			request,
			coverImage(multipartRequest)
		);
		return BookMapper.toResponse(book, bookService.getChapterStats(book));
	}

	@DeleteMapping("/{bookId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@PreAuthorize("hasRole('AUTHOR')")
	public void deleteBook(Principal principal, @PathVariable UUID bookId) {
		bookService.deleteBook(principal.getName(), bookId);
	}

	@PatchMapping("/{bookId}/ready")
	@PreAuthorize("hasRole('AUTHOR')")
	public BookResponse markReady(Principal principal, @PathVariable UUID bookId) {
		Book book = bookService.markReady(principal.getName(), bookId);
		return BookMapper.toResponse(book, bookService.getChapterStats(book));
	}

	@PatchMapping("/{bookId}/publish")
	@PreAuthorize("hasRole('AUTHOR')")
	public BookResponse publishBook(Principal principal, @PathVariable UUID bookId) {
		Book book = bookService.publishBook(principal.getName(), bookId);
		return BookMapper.toResponse(book, bookService.getChapterStats(book));
	}

	@PatchMapping("/{bookId}/archive")
	@PreAuthorize("hasRole('AUTHOR')")
	public BookResponse archiveBook(Principal principal, @PathVariable UUID bookId) {
		Book book = bookService.archiveBook(principal.getName(), bookId);
		return BookMapper.toResponse(book, bookService.getChapterStats(book));
	}

	private CreateBookRequest createBookRequest(MultipartHttpServletRequest request) {
		CreateBookRequest jsonRequest = jsonPart(request, "book", CreateBookRequest.class);
		if (jsonRequest != null) {
			return jsonRequest;
		}
		return new CreateBookRequest(
			parameter(request, "title"),
			parameter(request, "subtitle"),
			parameter(request, "summary"),
			parameter(request, "coverUrl", "cover_url", "coverImageUrl", "cover_image_url", "imageUrl", "image_url"),
			parameter(request, "genre"),
			parameter(request, "languageCode", "language_code")
		);
	}

	private UpdateBookRequest updateBookRequest(MultipartHttpServletRequest request) {
		UpdateBookRequest jsonRequest = jsonPart(request, "book", UpdateBookRequest.class);
		if (jsonRequest != null) {
			return jsonRequest;
		}
		return new UpdateBookRequest(
			parameter(request, "title"),
			parameter(request, "subtitle"),
			parameter(request, "summary"),
			parameter(request, "coverUrl", "cover_url", "coverImageUrl", "cover_image_url", "imageUrl", "image_url"),
			parameter(request, "genre"),
			parameter(request, "languageCode", "language_code")
		);
	}

	private MultipartFile coverImage(MultipartHttpServletRequest request) {
		for (String partName : List.of("coverImage", "cover_image", "image", "imageFile", "cover", "file")) {
			MultipartFile file = request.getFile(partName);
			if (file != null && !file.isEmpty()) {
				return file;
			}
		}
		return request.getFileMap()
			.entrySet()
			.stream()
			.filter(entry -> !"book".equals(entry.getKey()))
			.map(java.util.Map.Entry::getValue)
			.filter(file -> file != null && !file.isEmpty())
			.findFirst()
			.orElse(null);
	}

	private String parameter(MultipartHttpServletRequest request, String... names) {
		for (String name : names) {
			String value = request.getParameter(name);
			if (StringUtils.hasText(value)) {
				return value;
			}
		}
		return null;
	}

	private <T> T jsonPart(MultipartHttpServletRequest request, String partName, Class<T> type) {
		String parameter = request.getParameter(partName);
		if (StringUtils.hasText(parameter)) {
			return readJson(parameter, type);
		}

		MultipartFile part = request.getFile(partName);
		if (part == null || part.isEmpty()) {
			return null;
		}
		try {
			return objectMapper.readValue(part.getBytes(), type);
		} catch (IOException exception) {
			throw new BusinessException("Invalid book multipart payload");
		}
	}

	private <T> T readJson(String value, Class<T> type) {
		try {
			return objectMapper.readValue(value, type);
		} catch (IOException exception) {
			throw new BusinessException("Invalid book multipart payload");
		}
	}

	private <T> T validate(T request) {
		Set<ConstraintViolation<T>> violations = validator.validate(request);
		if (!violations.isEmpty()) {
			String message = violations.stream()
				.map(violation -> violation.getPropertyPath() + ": " + violation.getMessage())
				.collect(Collectors.joining(", "));
			throw new BusinessException(message);
		}
		return request;
	}
}
