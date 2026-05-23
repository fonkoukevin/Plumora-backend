package com.plumora.api.book.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.plumora.api.shared.exception.BusinessException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;

class LocalBookCoverStorageTest {

	@TempDir
	private Path uploadDir;

	@Test
	void storesImageAndReturnsRelativeCoverUrl() throws Exception {
		LocalBookCoverStorage storage = new LocalBookCoverStorage(
			uploadDir.toString(),
			"uploads/book-covers",
			5_242_880
		);
		MockMultipartFile image = new MockMultipartFile(
			"coverImage",
			"cover.png",
			MediaType.IMAGE_PNG_VALUE,
			new byte[] {1, 2, 3}
		);

		String coverUrl = storage.store(image);

		assertThat(coverUrl).startsWith("uploads/book-covers/");
		String filename = coverUrl.substring("uploads/book-covers/".length());
		assertThat(Files.exists(uploadDir.resolve("book-covers").resolve(filename))).isTrue();
		assertThat(storage.load(filename).exists()).isTrue();
		assertThat(storage.mediaType(filename)).isEqualTo(MediaType.IMAGE_PNG);
	}

	@Test
	void rejectsNonImageFiles() {
		LocalBookCoverStorage storage = new LocalBookCoverStorage(
			uploadDir.toString(),
			"uploads/book-covers",
			5_242_880
		);
		MockMultipartFile textFile = new MockMultipartFile(
			"coverImage",
			"cover.txt",
			MediaType.TEXT_PLAIN_VALUE,
			new byte[] {1, 2, 3}
		);

		assertThatThrownBy(() -> storage.store(textFile))
			.isInstanceOf(BusinessException.class)
			.hasMessage("Book cover must be a JPEG, PNG, WebP or GIF image");
	}
}
