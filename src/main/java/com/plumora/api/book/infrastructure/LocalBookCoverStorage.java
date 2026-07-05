package com.plumora.api.book.infrastructure;

import com.plumora.api.book.application.BookCoverStorage;
import com.plumora.api.shared.exception.BusinessException;
import com.plumora.api.shared.exception.ResourceNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
public class LocalBookCoverStorage implements BookCoverStorage {
	private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
		MediaType.IMAGE_JPEG_VALUE,
		MediaType.IMAGE_PNG_VALUE,
		"image/webp",
		"image/gif"
	);
	private static final Map<String, String> CONTENT_TYPE_EXTENSIONS = Map.of(
		MediaType.IMAGE_JPEG_VALUE, "jpg",
		MediaType.IMAGE_PNG_VALUE, "png",
		"image/webp", "webp",
		"image/gif", "gif"
	);

	private final Path coverDirectory;
	private final String publicBasePath;
	private final long maxFileSizeBytes;

	public LocalBookCoverStorage(
		@Value("${app.storage.upload-dir:uploads}") String uploadDir,
		@Value("${app.storage.book-cover-public-path:uploads/book-covers}") String publicBasePath,
		@Value("${app.storage.max-book-cover-size-bytes:5242880}") long maxFileSizeBytes
	) {
		this.coverDirectory = Paths.get(uploadDir).toAbsolutePath().normalize().resolve("book-covers");
		this.publicBasePath = trimSlashes(publicBasePath);
		this.maxFileSizeBytes = maxFileSizeBytes;
	}

	@Override
	public String store(MultipartFile file) {
		if (file == null || file.isEmpty()) {
			throw new BusinessException("Book cover image is empty");
		}
		validate(file);

		String extension = extension(file);
		String filename = UUID.randomUUID() + "." + extension;
		Path destination = coverDirectory.resolve(filename).normalize();

		try {
			Files.createDirectories(coverDirectory);
			try (InputStream inputStream = file.getInputStream()) {
				Files.copy(inputStream, destination, StandardCopyOption.REPLACE_EXISTING);
			}
			return publicBasePath + "/" + filename;
		} catch (IOException exception) {
			throw new BusinessException("Book cover image could not be stored");
		}
	}

	@Override
	public Resource load(String filename) {
		if (!StringUtils.hasText(filename) || filename.contains("/") || filename.contains("\\")) {
			throw new ResourceNotFoundException("Book cover image was not found");
		}

		Resource uploadedCover = loadUploadedCover(filename);
		if (uploadedCover != null) {
			return uploadedCover;
		}

		Resource bundledCover = new ClassPathResource("static/" + publicBasePath + "/" + filename);
		if (bundledCover.exists() && bundledCover.isReadable()) {
			return bundledCover;
		}

		throw new ResourceNotFoundException("Book cover image was not found");
	}

	private Resource loadUploadedCover(String filename) {
		try {
			Path file = coverDirectory.resolve(filename).normalize();
			if (!file.startsWith(coverDirectory) || !Files.exists(file)) {
				return null;
			}
			Resource resource = new UrlResource(file.toUri());
			if (!resource.exists() || !resource.isReadable()) {
				return null;
			}
			return resource;
		} catch (MalformedURLException exception) {
			return null;
		}
	}

	@Override
	public MediaType mediaType(String filename) {
		return MediaTypeFactory.getMediaType(filename).orElse(MediaType.APPLICATION_OCTET_STREAM);
	}

	private void validate(MultipartFile file) {
		if (file.getSize() > maxFileSizeBytes) {
			throw new BusinessException("Book cover image must be 5 MB or smaller");
		}
		String contentType = file.getContentType();
		if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase(Locale.ROOT))) {
			throw new BusinessException("Book cover must be a JPEG, PNG, WebP or GIF image");
		}
	}

	private String extension(MultipartFile file) {
		String contentType = file.getContentType() == null ? "" : file.getContentType().toLowerCase(Locale.ROOT);
		String extension = CONTENT_TYPE_EXTENSIONS.get(contentType);
		if (extension != null) {
			return extension;
		}

		String originalExtension = StringUtils.getFilenameExtension(file.getOriginalFilename());
		if (originalExtension == null) {
			return "img";
		}
		return originalExtension.toLowerCase(Locale.ROOT);
	}

	private static String trimSlashes(String value) {
		String result = value == null ? "uploads/book-covers" : value.trim();
		while (result.startsWith("/")) {
			result = result.substring(1);
		}
		while (result.endsWith("/")) {
			result = result.substring(0, result.length() - 1);
		}
		return result.isBlank() ? "uploads/book-covers" : result;
	}
}
